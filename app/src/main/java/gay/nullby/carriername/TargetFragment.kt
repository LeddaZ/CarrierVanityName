package gay.nullby.carriername

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyFrameworkInitializer
import android.telephony.TelephonyManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.android.internal.telephony.ICarrierConfigLoader
import gay.nullby.carriername.databinding.FragmentTargetBinding
import rikka.shizuku.ShizukuBinderWrapper
import java.util.Locale

class TargetFragment : Fragment() {
    private val tag: String = "TargetFragment"

    private var _binding: FragmentTargetBinding? = null

    private val binding get() = _binding!!

    private var subId1: Int = -1
    private var subId2: Int = -1

    private var selectedSub: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTargetBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subId1 = SubscriptionManager.getSubscriptionId(0)
        subId2 = SubscriptionManager.getSubscriptionId(1)

        val sub1button = view.findViewById<RadioButton>(R.id.sub1_button)
        val sub2button = view.findViewById<RadioButton>(R.id.sub2_button)

        Log.d(tag, "#onViewCreated(): subId1=$subId1 subId2=$subId2")

        sub1button.text = getString(R.string.sub_button, "1", getCarrierNameBySubId(subId1))
        sub2button.text = getString(R.string.sub_button, "2", getCarrierNameBySubId(subId2))

        if (subId2 == -1) {
            sub2button.visibility = View.GONE
        }

        view.findViewById<Button>(R.id.button_set).setOnClickListener { onSetName(view.findViewById<EditText>(R.id.text_entry).text.toString(), view.findViewById<EditText>(R.id.iso_region_input).text.toString()) }

        view.findViewById<Button>(R.id.button_reset).setOnClickListener {
            onResetName()
            view.findViewById<EditText>(R.id.text_entry).setText("")
            view.findViewById<EditText>(R.id.iso_region_input).setText("")
        }

        view.findViewById<RadioGroup>(R.id.sub_selection).setOnCheckedChangeListener { _, checkedId -> onSelectSub(checkedId) }

        onSelectSub(0)
    }

    private fun onSetName(text: String, isoRegion: String) {
        val p = PersistableBundle()
        if (isoRegion.isNotEmpty()) {
            if (isoRegion.length == 2) {
                p.putString(CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING, isoRegion.lowercase(Locale.ROOT)
                )
            } else {
                Toast.makeText(context, getString(R.string.invalid_iso), Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(context, getString(R.string.name_set, text), Toast.LENGTH_SHORT).show()

        p.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
        p.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, text)
        p.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, /* trans rights! 🏳️‍⚧️*/ ":3")
        p.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
        val subId: Int = if (selectedSub == 1) {
            subId1
        } else {
            subId2
        }
        overrideCarrierConfig(subId, p)
    }

    private fun onResetName() {
        val p = PersistableBundle()
        p.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)
        p.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, "")
        val subId: Int = if (selectedSub == 1) {
            subId1
        } else {
            subId2
        }
        // Sometimes just setting the override to null doesn't work, so let's first set another override, disabling the name change
        overrideCarrierConfig(subId, p)
        overrideCarrierConfig(subId, null)
    }

    private fun onSelectSub(id: Int) {
        if (id == R.id.sub1_button || id == 0) {
            selectedSub = 1
            Toast.makeText(context, getString(R.string.network_selected, "1"), Toast.LENGTH_SHORT).show()
        } else if (id == R.id.sub2_button) {
            selectedSub = 2
            Toast.makeText(context, getString(R.string.network_selected, "2"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCarrierNameBySubId(subId: Int): String {
        val telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return ""
        return telephonyManager.getNetworkOperatorName(subId)
    }

    private fun overrideCarrierConfig(subId: Int, p: PersistableBundle?) {
        val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(
            TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .carrierConfigServiceRegisterer
                .get()?.let {
                    ShizukuBinderWrapper(
                        it
                    )
                }
        )
        carrierConfigLoader.overrideConfig(subId, p, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
