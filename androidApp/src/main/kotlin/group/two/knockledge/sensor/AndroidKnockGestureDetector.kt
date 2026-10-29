package group.two.knockledge.sensor

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import javax.inject.Inject
import kotlin.math.sqrt

class AndroidKnockGestureDetector @Inject constructor(app: Application) : KnockGestureDetector {
    private val sensors = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var thread: HandlerThread? = null
    private var listener: SensorEventListener? = null

    override val isListening: Boolean get() = listener != null

    override fun startListening(onKnockDetected: () -> Unit) {
        if (isListening) return
        val linear = sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val sensor = linear ?: sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val filter = if (linear == null) GravityHighPass() else null
        val matcher = KnockPeakMatcher()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val v = event.values
                val magnitude = filter?.magnitude(v[0], v[1], v[2])
                    ?: sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
                if (matcher.onSample(magnitude, event.timestamp / 1_000_000L)) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    onKnockDetected()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val t = HandlerThread("knock-sensor").also { it.start() }
        thread = t
        listener = l
        sensors.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME, Handler(t.looper))
    }

    override fun stopListening() {
        listener?.let { sensors.unregisterListener(it) }
        listener = null
        thread?.quitSafely()
        thread = null
    }
}

class GravityHighPass(private val alpha: Float = 0.8f) {
    private val lastIn = FloatArray(3)
    private val lastOut = FloatArray(3)

    fun magnitude(x: Float, y: Float, z: Float): Float {
        val input = floatArrayOf(x, y, z)
        for (i in 0..2) {
            lastOut[i] = alpha * (lastOut[i] + input[i] - lastIn[i])
            lastIn[i] = input[i]
        }
        return sqrt(lastOut[0] * lastOut[0] + lastOut[1] * lastOut[1] + lastOut[2] * lastOut[2])
    }
}
