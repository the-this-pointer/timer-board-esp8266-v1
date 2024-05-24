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

class WeekDayPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    val TAG: String = "WeekDayPicker"
    private var mPickDay: ImageView
    private var mDayTextView: TextView
    private var mTitleTextView: TextView
    var mDay: Int = -1
        get() = field

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.week_day_picker_layout, this, true)

        mDayTextView = findViewById(R.id.dayTextView)
        mTitleTextView = findViewById(R.id.titleTextView)
        mPickDay = findViewById(R.id.pickDayView)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                R.styleable.week_day_picker, 0, 0)
            val title = resources.getText(typedArray
                .getResourceId(R.styleable
                    .week_day_picker_week_day_picker_caption,
                    R.string.picker_time_default))

            mTitleTextView.text = title
            typedArray.recycle()
        }

        mPickDay.setOnClickListener {
            Log.d(TAG, "peek clicked!")

            val items = resources.getStringArray(R.array.week_days)
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Week Day")
            builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
                Log.d(TAG, "day clicked!")
                mDay = item
                mDayTextView.text = items[item]
                dialog.dismiss()
            })
            builder.show()
        }
    }
}