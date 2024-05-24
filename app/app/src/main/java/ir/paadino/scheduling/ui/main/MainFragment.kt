package ir.paadino.scheduling.ui.main

import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.github.tlaabs.timetableview.Schedule
import com.github.tlaabs.timetableview.Time
import com.github.tlaabs.timetableview.TimetableView
import ir.paadino.scheduling.R
import ir.paadino.scheduling.net.MixerPaad
import ir.paadino.scheduling.storage.ConfigurationManager
import ir.paadino.scheduling.ui.components.TimePicker
import ir.paadino.scheduling.ui.components.WeekDayPicker
import kotlinx.android.synthetic.main.main_fragment.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


class MainFragment : Fragment(), MixerPaad.MixerPaadListener{
    var menu: Menu? = null
    var timeTable: TimetableView? = null
    lateinit var mixerPaad: MixerPaad

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.main_fragment, container, false)

        timeTable = v.findViewById(R.id.timetable)
        timeTable?.setOnStickerSelectEventListener { idx, schedules ->
            deleteTime(idx)
        }
        if (ConfigurationManager.getInstance(context).scheduleData.length > 0)
            timeTable?.load(ConfigurationManager.getInstance(context).scheduleData)

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mixerPaad = MixerPaad(this)

        setHasOptionsMenu(true)
    }

    fun saveData() {
        if (!mixerPaad.isConnected) return

        val json = timeTable?.createSaveData()
        var output = JSONObject()
        val arr = JSONArray()

        val jObj: JSONObject = JSONObject(json!!)
        val stickers = jObj.getJSONArray("sticker")
        for (i in 0 until stickers.length()) {
            val o = stickers.getJSONObject(i)
            val s = o.getJSONArray("schedule")
            if (s.length() == 0) return;
            val schedule: JSONObject = s.getJSONObject(0)
            val day: Int = schedule.getInt("day")
            val startTime: JSONObject = schedule.getJSONObject("startTime")
            val endTime: JSONObject = schedule.getJSONObject("endTime")

            var jsonTimes = JSONObject()
            jsonTimes.put("day", day)
            jsonTimes.put("startTime", startTime)
            jsonTimes.put("endTime", endTime)
            arr.put(jsonTimes)
        }
        output.put("times", arr)

//        Log.d("Save DATA", "savedData: " + timeTable?.createSaveData())
        Log.d("Save DATA", "outputData: " + output.toString())
        mixerPaad.setData(output.toString())
    }

    fun saveDataOnDevice() {
        val json = timeTable?.createSaveData()
        ConfigurationManager.getInstance(context).scheduleData = json
    }

    override fun onStop() {
        if (mixerPaad.isConnected)
            mixerPaad.disconnect()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_time_table, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu

        menuDisconnectedState()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menu_connect -> {
                connect()
            }
            R.id.menu_add -> {
                addTime()
            }
            R.id.menu_disconnect -> {
                disconnect()
            }
            R.id.menu_set_time -> {
                setTime()
            }
            R.id.menu_set_ssid -> {
                setSSID()
            }
            R.id.menu_set_password -> {
                setPassword()
            }
            R.id.menu_load_data -> {
                loadData()
            }
        }

        return super.onOptionsItemSelected(item)
    }



    fun menuDisconnectedState() {
        activity?.runOnUiThread {
            var item = menu?.findItem(R.id.menu_add)
            item?.setVisible(false)
            item = menu?.findItem(R.id.menu_disconnect)
            item?.setVisible(false)
            item = menu?.findItem(R.id.menu_connect)
            item?.setVisible(true)
            item = menu?.findItem(R.id.menu_set_time)
            item?.setVisible(false)
            item = menu?.findItem(R.id.menu_set_ssid)
            item?.setVisible(false)
            item = menu?.findItem(R.id.menu_set_password)
            item?.setVisible(false)
            item = menu?.findItem(R.id.menu_load_data)
            item?.setVisible(false)
        }
    }

    fun menuConnectedState() {
        activity?.runOnUiThread {
            var item = menu?.findItem(R.id.menu_add)
            item?.setVisible(true)
            item = menu?.findItem(R.id.menu_disconnect)
            item?.setVisible(true)
            item = menu?.findItem(R.id.menu_connect)
            item?.setVisible(false)
            item = menu?.findItem(R.id.menu_set_time)
            item?.setVisible(true)
            item = menu?.findItem(R.id.menu_set_ssid)
            item?.setVisible(true)
            item = menu?.findItem(R.id.menu_set_password)
            item?.setVisible(true)
            item = menu?.findItem(R.id.menu_load_data)
            item?.setVisible(true)
        }
    }

    fun connect() {
        mixerPaad.connect()
    }

    fun disconnect() {
        mixerPaad.disconnect()
    }

    fun addTime() {
        if (!mixerPaad.isConnected) {
            Toast.makeText(activity, "Device not connected.", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(activity!!)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.add_time_layout, null)
        val dayPicker: WeekDayPicker = dialogView.findViewById<WeekDayPicker>(R.id.dayPicker)
        val fromPicker: TimePicker = dialogView.findViewById<TimePicker>(R.id.fromPicker)
        val toPicker: TimePicker = dialogView.findViewById<TimePicker>(R.id.toPicker)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, which ->
            //Do nothing here!
            //We override this later...
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which ->
            // Do nothing
            dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var valid = true
            if (dayPicker.mDay == -1)
                valid = false
            if (fromPicker.mHour == -1 || fromPicker.mMinute == -1)
                valid = false
            if (toPicker.mHour == -1 || toPicker.mMinute == -1)
                valid = false

            if (!valid) {
                Toast.makeText(activity, "Select all values.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fromPicker.mHour > toPicker.mHour) {
                Toast.makeText(activity, "Invalid time range. Edit hours again!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val schedules = ArrayList<Schedule>()
            val schedule = Schedule()
            schedule.classTitle = String.format("%d:%d - %d:%d", fromPicker.mHour, fromPicker.mMinute, toPicker.mHour, toPicker.mMinute) // sets subject
            schedule.classPlace = "" // sets place
            schedule.professorName = "" // sets professor
            schedule.startTime =
                Time(fromPicker.mHour, fromPicker.mMinute) // sets the beginning of class time (hour,minute)
            schedule.endTime = Time(toPicker.mHour, toPicker.mMinute) // sets the end of class time (hour,minute)
            schedule.day = dayPicker.mDay
            schedules.add(schedule)
            //.. add one or more schedules
            timetable.add(schedules)
            saveData()

            alertDialog.dismiss()
        }

        /*var day: Int = 0;var fHour: Int = 0;var fMinute: Int = 0;var tHour: Int = 0;var tMinute: Int = 0
        val cal = Calendar.getInstance()
        val toTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            tHour = hour
            tMinute = minute

            val schedules = ArrayList<Schedule>()
            val schedule = Schedule()
            schedule.classTitle = String.format("%d:%d - %d:%d", fHour, fMinute, tHour, tMinute) // sets subject
            schedule.classPlace = "" // sets place
            schedule.professorName = "" // sets professor
            schedule.startTime =
                Time(fHour, fMinute) // sets the beginning of class time (hour,minute)
            schedule.endTime = Time(tHour, tMinute) // sets the end of class time (hour,minute)
            schedule.day = day
            schedules.add(schedule)
            //.. add one or more schedules
            timetable.add(schedules)
            saveData()
        }

        val fromTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            fHour = hour
            fMinute = minute
            TimePickerDialog(context, toTimeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        val items = arrayOf<CharSequence>("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
        val builder = AlertDialog.Builder(this@MainFragment.activity!!)
        builder.setTitle("Week Day")
        builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
            // Do something with the selection
            day = item
            dialog.dismiss()
            TimePickerDialog(context, fromTimeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        })
        builder.show()*/
    }

    fun deleteTime(idx: Int){
        if (!mixerPaad.isConnected) {
            Toast.makeText(activity, "Device not connected.", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle("Delete")
        builder.setMessage("Are you sure?")
        builder.setPositiveButton("YES") { dialog, which ->
            timeTable?.remove(idx)
            saveData()
            dialog.dismiss()
        }
        builder.setNegativeButton(
            "NO"
        ) { dialog, which ->
            // Do nothing
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    fun setTime() {
        if (!mixerPaad.isConnected) {
            Toast.makeText(activity, "Device not connected.", Toast.LENGTH_SHORT).show()
            return
        }

        var wday: Int = 0
        val cal = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            mixerPaad.setTime(hour, minute, wday)
        }

        val items = arrayOf<CharSequence>("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
        val builder = AlertDialog.Builder(this@MainFragment.activity!!)
        builder.setTitle("Week Day")
        builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
            // Do something with the selection
            wday = item
            dialog.dismiss()
            TimePickerDialog(context, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        })
        builder.show()
    }

    fun setSSID() {
        if (!mixerPaad.isConnected) {
            Toast.makeText(activity, "Device not connected.", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(context)
        editText.setSingleLine()
        val container = FrameLayout(context!!)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = 30
        params.rightMargin = 30
        editText.setLayoutParams(params)
        container.addView(editText)
        val dialog: AlertDialog  = AlertDialog.Builder(context!!)
            .setTitle("Set SSID")
            .setMessage("Enter SSID")
            .setView(container)
            .setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
                val editTextInput:String  = editText.text.toString()
                mixerPaad.setSsid(editTextInput)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }


    fun setPassword() {
        if (!mixerPaad.isConnected) {
            Toast.makeText(activity, "Device not connected.", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(context)
        editText.setSingleLine()
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val container = FrameLayout(context!!)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = 30
        params.rightMargin = 30
        editText.setLayoutParams(params)
        container.addView(editText)
        val dialog: AlertDialog  = AlertDialog.Builder(context!!)
            .setTitle("Set Password")
            .setMessage("Enter password")
            .setView(container)
            .setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
                val editTextInput:String  = editText.text.toString()

                if (editTextInput.length < 8 || editTextInput.length > 12) {
                    Toast.makeText(activity, "Invalid password!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                mixerPaad.setPassword(editTextInput)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    fun loadData() {
        if (!mixerPaad.isConnected) {
            Toast.makeText(activity, "Device not connected.", Toast.LENGTH_SHORT).show()
            return
        }

        mixerPaad.requestReadData()
    }

    override fun onConnected() {
        Log.d("MixerPaad", "onConnected ")
        menuConnectedState()
    }

    override fun onDisconnected() {
        Log.d("MixerPaad", "onDisconnected ")
        menuDisconnectedState()
    }

    override fun onPasswordReturned(password: String?) {
        Log.d("MixerPaad", "onPasswordReturned "+password!!)
    }

    override fun onError(error: String?) {
        Log.d("MixerPaad", "onError "+error!!)
    }

    override fun ping(ping: Long) {
        Log.d("MixerPaad", "ping $ping")
    }

    override fun onDataSaved() {
        Log.d("MixerPaad", "onDataSaved")
        saveDataOnDevice()
    }

    override fun onDataReceived(data: String?) {
        Log.d("MixerPaad", "onDataReturned "+data!!)

        val jObject = JSONObject(data)
        if (!jObject.has("times")) return

        activity?.runOnUiThread {
            // {"times":[{"endTime":{"hour":18,"minute":53},"startTime":{"hour":18,"minute":52},"day":6},{"endTime":{"hour":12,"minute":5},"startTime":{"hour":11,"minute":45},"day":2}]}*
            timetable.removeAll()
            val arr: JSONArray= jObject.getJSONArray("times")
            for (i in 0 until arr.length()) {
                val json = arr.getJSONObject(i)
                if (!json.has("day")) continue;

                val day = json.getInt("day")
                val startTime: JSONObject = json.getJSONObject("startTime")
                val endTime: JSONObject = json.getJSONObject("endTime")
                val fHour = startTime.getInt("hour")
                val fMinute = startTime.getInt("minute")
                val tHour = endTime.getInt("hour")
                val tMinute = endTime.getInt("minute")

                val schedules = ArrayList<Schedule>()
                val schedule = Schedule()
                schedule.classTitle = String.format("%d:%d - %d:%d", fHour, fMinute, tHour, tMinute) // sets subject
                schedule.classPlace = "" // sets place
                schedule.professorName = "" // sets professor
                schedule.startTime =
                    Time(fHour, fMinute) // sets the beginning of class time (hour,minute)
                schedule.endTime = Time(tHour, tMinute) // sets the end of class time (hour,minute)
                schedule.day = day
                schedules.add(schedule)
                //.. add one or more schedules
                timetable.add(schedules)
            }
            saveDataOnDevice()
        }
    }

    override fun onTimeSaved() {
        Log.d("MixerPaad", "onTimeSaved")
    }

    override fun onSSIDSaved() {
        Log.d("MixerPaad", "onSSIDSaved")
    }

    override fun onPasswordSaved() {
        Log.d("MixerPaad", "onPasswordSaved")
    }

}
