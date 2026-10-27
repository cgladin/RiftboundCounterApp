package com.cgladin.riftboundcounter.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cgladin.riftboundcounter.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val ROLL_MILLIS = 2000
private const val TEXT_UNIT = 100f

private class Vec(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = Vec(x * k, y * k, z * k)
    fun dot(o: Vec) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec) = Vec(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun normalized(): Vec {
        val l = sqrt(dot(this))
        return Vec(x / l, y / l, z / l)
    }
}

/** Matrice de rotation 3x3, stockée ligne par ligne. */
private class Rot(val m: FloatArray) {
    operator fun times(o: Rot): Rot {
        val r = FloatArray(9)
        for (i in 0..2) for (j in 0..2) {
            r[i * 3 + j] = m[i * 3] * o.m[j] + m[i * 3 + 1] * o.m[3 + j] + m[i * 3 + 2] * o.m[6 + j]
        }
        return Rot(r)
    }

    fun apply(v: Vec) = Vec(
        m[0] * v.x + m[1] * v.y + m[2] * v.z,
        m[3] * v.x + m[4] * v.y + m[5] * v.z,
        m[6] * v.x + m[7] * v.y + m[8] * v.z,
    )

    companion object {
        val Identity = Rot(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        fun axisAngle(axis: Vec, angle: Float): Rot {
            val a = axis.normalized()
            val c = cos(angle)
            val s = sin(angle)
            val t = 1 - c
            return Rot(
                floatArrayOf(
                    t * a.x * a.x + c, t * a.x * a.y - s * a.z, t * a.x * a.z + s * a.y,
                    t * a.x * a.y + s * a.z, t * a.y * a.y + c, t * a.y * a.z - s * a.x,
                    t * a.x * a.z - s * a.y, t * a.y * a.z + s * a.x, t * a.z * a.z + c,
                ),
            )
        }
    }
}

/** Icosaèdre régulier : 12 sommets, 20 faces numérotées avec des faces opposées de somme 21. */
private object D20 {
    private const val EDGE_SQUARED = 4f
    const val CIRCUMRADIUS = 1.9021f
    const val INRADIUS = 0.5774f

    val vertices: List<Vec>
    val faces: List<IntArray>
    val normals: List<Vec>
    val centroids: List<Vec>
    val numbers = IntArray(20)

    init {
        val phi = (1f + sqrt(5f)) / 2f
        val v = mutableListOf<Vec>()
        for (a in listOf(-1f, 1f)) for (b in listOf(-phi, phi)) {
            v += Vec(0f, a, b)
            v += Vec(a, b, 0f)
            v += Vec(b, 0f, a)
        }
        vertices = v
        fun d2(i: Int, j: Int): Float {
            val d = v[i] - v[j]
            return d.dot(d)
        }
        fun near(i: Int, j: Int) = abs(d2(i, j) - EDGE_SQUARED) < 0.01f
        val f = mutableListOf<IntArray>()
        for (i in v.indices) for (j in i + 1 until v.size) for (k in j + 1 until v.size) {
            if (near(i, j) && near(j, k) && near(i, k)) f += intArrayOf(i, j, k)
        }
        faces = f
        centroids = f.map { (v[it[0]] + v[it[1]] + v[it[2]]) * (1f / 3f) }
        normals = centroids.map { it.normalized() }
        var next = 1
        for (i in f.indices) {
            if (numbers[i] != 0) continue
            val opposite = normals.indices.first { normals[i].dot(normals[it]) < -0.99f }
            numbers[i] = next
            numbers[opposite] = 21 - next
            next++
        }
    }

    fun faceOf(value: Int) = numbers.indexOf(value)

    /** Rotation qui place la face [face] face à la caméra, avec une rotation de [roll] dans le plan. */
    fun faceUp(face: Int, roll: Float): Rot {
        val n = normals[face]
        val z = Vec(0f, 0f, 1f)
        val d = n.dot(z)
        val align = when {
            d > 0.9999f -> Rot.Identity
            d < -0.9999f -> Rot.axisAngle(Vec(1f, 0f, 0f), PI.toFloat())
            else -> Rot.axisAngle(n.cross(z), acos(d))
        }
        return Rot.axisAngle(z, roll) * align
    }
}

private class RollSpec(
    val axis1: Vec,
    val turns1: Float,
    val axis2: Vec,
    val turns2: Float,
    val fromX: Float,
    val start: Offset,
    val bounces: Int,
    val final: Rot,
)

private fun randomAxis() = Vec(
    Random.nextFloat() * 2 - 1,
    Random.nextFloat() * 2 - 1,
    Random.nextFloat() * 2 - 1,
).let { if (it.dot(it) < 0.01f) Vec(1f, 0f, 0f) else it }

/**
 * Affiche un dé à 20 faces en 3D ; chaque appui le lance dans le panneau puis affiche le résultat.
 *
 * @param result résultat du dernier lancer, ou null si le dé n’a pas encore été lancé.
 * @param color couleur de fond du panneau.
 * @param onResult callback appelé avec le résultat à la fin du lancer.
 * @param modifier modificateur Compose appliqué au panneau.
 */
@Composable
fun DicePanel(
    result: Int?,
    color: Color,
    onResult: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(1f) }
    var isRolling by remember { mutableStateOf(false) }
    var spec by remember { mutableStateOf<RollSpec?>(null) }
    val restRotation = remember { D20.faceUp(D20.faceOf(result ?: 20), 0.25f) }
    var orientation by remember { mutableStateOf(restRotation) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var shownResult by remember { mutableStateOf(result) }

    val dieColor = lerp(color, Color.White, 0.8f)
    val ink = lerp(color, Color.Black, 0.65f)
    val rollHint = stringResource(R.string.dice_roll_hint)
    val textPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = TEXT_UNIT
        }
    }
    val textMatrix = remember { android.graphics.Matrix() }

    fun roll(start: Offset = Offset.Zero) {
        isRolling = true
        shownResult = null
        val value = Random.nextInt(1, 21)
        val dir = if (Random.nextBoolean()) 1f else -1f
        val thrown = start != Offset.Zero
        val rollSpec = RollSpec(
            axis1 = randomAxis(),
            turns1 = 3f + Random.nextFloat() * 2f,
            axis2 = randomAxis(),
            turns2 = 2f + Random.nextFloat() * 2f,
            fromX = if (thrown) 0f else dir * (0.8f + Random.nextFloat() * 0.2f),
            start = start,
            bounces = 3,
            final = D20.faceUp(D20.faceOf(value), Random.nextFloat() * 2f * PI.toFloat()),
        )
        spec = rollSpec
        dragOffset = Offset.Zero
        scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(ROLL_MILLIS, easing = LinearOutSlowInEasing))
            orientation = rollSpec.final
            shownResult = value
            onResult(value)
            isRolling = false
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .pointerInput(Unit) {
                detectTapGestures { if (!isRolling) roll() }
            }
            .pointerInput(Unit) {
                // Le dé suit le doigt sans tourner ; il est lancé depuis sa position au relâchement.
                detectDragGestures(
                    onDragEnd = { if (!isRolling) roll(dragOffset) },
                    onDragCancel = { if (!isRolling) roll(dragOffset) },
                ) { change, drag ->
                    change.consume()
                    if (!isRolling) {
                        val limitX = size.width / 2f - 0.27f * min(size.width, size.height)
                        val limitY = size.height / 2f - 0.27f * min(size.width, size.height)
                        dragOffset = Offset(
                            (dragOffset.x + drag.x).coerceIn(-limitX, limitX),
                            (dragOffset.y + drag.y).coerceIn(-limitY, limitY),
                        )
                    }
                }
            }
            .semantics {
                role = Role.Button
                onClick(label = rollHint) {
                    if (isRolling) {
                        false
                    } else {
                        roll()
                        true
                    }
                }
            },
    ) {
        shownResult?.let {
            Text(
                text = it.toString(),
                color = Palette.onPlayer(color),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val s = spec
            val p = progress.value
            val remaining = 1f - p
            val radiusPx = 0.27f * min(size.width, size.height)
            val scale = radiusPx / D20.CIRCUMRADIUS
            val rotation: Rot
            var dx = 0f
            var dy = 0f
            var lift = 0f
            if (s == null || !isRolling) {
                rotation = orientation
                dx = dragOffset.x
                dy = dragOffset.y
            } else {
                val twoPi = 2f * PI.toFloat()
                rotation = Rot.axisAngle(s.axis1, s.turns1 * twoPi * remaining) *
                    Rot.axisAngle(s.axis2, s.turns2 * twoPi * remaining) * s.final
                dx = s.fromX * (size.width / 2f - radiusPx * 1.1f) * remaining + s.start.x * remaining
                dy = s.start.y * remaining
                lift = abs(sin(s.bounces * PI.toFloat() * p)) * remaining * remaining
            }
            val cx = size.width / 2f + dx
            val groundY = size.height / 2f + dy
            val cy = groundY - lift * 0.2f * size.height

            // Ombre au sol, plus petite et plus pâle quand le dé est en l’air.
            val shadowW = radiusPx * 1.5f * (1f - 0.3f * lift)
            val shadowH = radiusPx * 0.3f * (1f - 0.3f * lift)
            drawOval(
                Color.Black.copy(alpha = 0.28f * (1f - 0.6f * lift)),
                topLeft = Offset(cx - shadowW / 2f, groundY + radiusPx * 0.95f - shadowH / 2f),
                size = Size(shadowW, shadowH),
            )

            fun project(v: Vec) = Offset(cx + v.x * scale, cy - v.y * scale)
            val light = Vec(-0.4f, 0.6f, 0.7f).normalized()

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                for (i in D20.faces.indices) {
                    val n = rotation.apply(D20.normals[i])
                    if (n.z <= 0f) continue
                    val idx = D20.faces[i]
                    val pts = idx.map { project(rotation.apply(D20.vertices[it])) }
                    val brightness = 0.55f + 0.45f * max(0f, n.dot(light))
                    val face = Color(dieColor.red * brightness, dieColor.green * brightness, dieColor.blue * brightness)
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        lineTo(pts[1].x, pts[1].y)
                        lineTo(pts[2].x, pts[2].y)
                        close()
                    }
                    drawPath(path, face)
                    drawPath(path, ink, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))

                    val up = (D20.vertices[idx[0]] - D20.centroids[i]).normalized()
                    val right = up.cross(D20.normals[i])
                    val c = rotation.apply(D20.centroids[i])
                    val unit = 0.55f
                    val pc = project(c)
                    val pr = project(c + rotation.apply(right) * unit)
                    val pu = project(c + rotation.apply(up) * unit)
                    textMatrix.setPolyToPoly(
                        floatArrayOf(0f, 0f, TEXT_UNIT, 0f, 0f, -TEXT_UNIT),
                        0,
                        floatArrayOf(pc.x, pc.y, pr.x, pr.y, pu.x, pu.y),
                        0,
                        3,
                    )
                    textPaint.color = ink.toArgb()
                    native.save()
                    native.concat(textMatrix)
                    native.drawText(D20.numbers[i].toString(), 0f, TEXT_UNIT * 0.36f, textPaint)
                    native.restore()
                }
            }
        }
    }
}
