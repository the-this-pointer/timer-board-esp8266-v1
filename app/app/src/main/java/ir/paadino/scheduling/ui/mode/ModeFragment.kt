package ir.paadino.scheduling.ui.mode

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
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import ir.paadino.scheduling.net.MixerPaad
import com.polyak.iconswitch.IconSwitch
import com.polyak.iconswitch.IconSwitch.Checked

import ir.paadino.scheduling.util.*
import ir.paadino.scheduling.storage.ConfigurationManager
import ir.paadino.scheduling.R
import kotlinx.android.synthetic.main.mode_fragment.*

class ModeFragment : Fragment(), IconSwitch.CheckedChangeListener {
    companion object {
        fun newInstance() = ModeFragment()
    }

    private var mRunning = false
    private lateinit var viewModel: ModeViewModel
    lateinit var mixerPaad: MixerPaad
    lateinit var modeSwitch: IconSwitch

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.mode_fragment, container, false)

        modeSwitch = v.findViewById(R.id.mode_switch);
        modeSwitch.setCheckedChangeListener(this);

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCheckChanged(current: Checked) {
        when(modeSwitch.getChecked()){
            Checked.LEFT -> {
                // stop
                mixerPaad.setState(false);
            }
            Checked.RIGHT -> {
                // play
                mixerPaad.setState(true);
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ModeViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
