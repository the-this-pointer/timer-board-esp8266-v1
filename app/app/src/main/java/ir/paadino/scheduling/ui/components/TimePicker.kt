package ir.paadino.scheduling.ui.components

import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import ir.paadino.scheduling.R
import java.util.*

class TimePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    val TAG: String = "TimePicker"
    private var mPickTime: ImageView
    private var mTimeTextView: TextView
    private var mTitleTextView: TextView

    var mTimeListener: TimePickerDialog.OnTimeSetListener? = null
        set(value) {
            field = value
        }

    var mHour: Int = -1
        get() = field
        set(value) {
            field = value
        }

    var mMinute: Int = -1
        get() = field
        set(value) {
            field = value
        }

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.time_picker_layout, this, true)

        mTimeTextView = findViewById(R.id.timeTextView)
        mTitleTextView = findViewById(R.id.titleTextView)
        mPickTime = findViewById(R.id.pickTimeView)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                R.styleable.time_picker, 0, 0)
            val title = resources.getText(typedArray
                .getResourceId(R.styleable
                    .time_picker_time_picker_caption,
                    R.string.picker_time_default))

            mTitleTextView.text = title
            typedArray.recycle()
        }

        mPickTime.setOnClickListener {
            Log.d(TAG, "peek clicked!")

            showDialog()
        }
    }

    fun showDialog() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            mHour = hour
            mMinute = minute
            mTimeTextView.text = String.format("%02d : %02d", hour, minute)
            mTimeListener?.onTimeSet(timePicker, hour, minute)
        }
        val cal = Calendar.getInstance()
        TimePickerDialog(context, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }


}