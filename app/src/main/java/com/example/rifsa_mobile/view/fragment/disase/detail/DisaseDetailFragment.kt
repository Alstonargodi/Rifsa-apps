package com.example.rifsa_mobile.view.fragment.disase.detail

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rifsa_mobile.databinding.FragmentDisaseDetailBinding
import com.example.rifsa_mobile.model.entity.disase.Disease
import com.example.rifsa_mobile.utils.AlarmReceiver
import com.example.rifsa_mobile.utils.Utils
import com.example.rifsa_mobile.viewmodel.LocalViewModel
import com.example.rifsa_mobile.viewmodel.utils.ObtainViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

//todo 1.4 reminder for do healing | done
//todo (bug) location request
class DisaseDetailFragment : Fragment() {
    private lateinit var binding : FragmentDisaseDetailBinding
    private lateinit var viewModel: LocalViewModel

    private lateinit var alarmReceive : AlarmReceiver
    private var alarmID = (1..1000).random()

    private var randomId = Utils.randomId()
    private var image = ""
    private var isDetail = false
    private var sortId = 0

    private var curLatitude = 0.0
    private var curLongitude = 0.0

    private val fineLocation =
        android.Manifest.permission.ACCESS_FINE_LOCATION
    private val coarseLocation =
        android.Manifest.permission.ACCESS_COARSE_LOCATION

    private lateinit var fusedLocation :
            FusedLocationProviderClient

    private lateinit var locationRequest :
            LocationRequest

    //launch after permit granted
    private var requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ){ permission ->
            when{
                permission[fineLocation] ?: false -> {
                    lifecycleScope.launch {
                        getCurrentLocation()
                    }
                }
                permission[coarseLocation] ?: false ->{
                    lifecycleScope.launch {
                        getCurrentLocation()
                    }
                }
                else ->{}
            }
        }

    //launch and check permission
    private fun checkPermission(permission : String): Boolean{
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisaseDetailBinding.inflate(layoutInflater)
        viewModel = ObtainViewModel(requireActivity())
        alarmReceive = AlarmReceiver()
        fusedLocation =
            LocationServices.getFusedLocationProviderClient(requireContext())

        createLocationRequest()

        try {
            showImage()
            val detail = DisaseDetailFragmentArgs.fromBundle(requireArguments()).diseaseDetail
            if (detail != null){
                isDetail = true
                randomId = detail.id_disease
                alarmID  = detail.reminderID
                sortId = detail.id_sort
                binding.btnDiseaseComplete.visibility = View.VISIBLE
            }
        }catch (e : Exception){ }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnDiseaseSave.setOnClickListener {
            lifecycleScope.launch {
                insertDiseaseLocal()
            }
        }

        binding.btnDiseaseBackhome.setOnClickListener {
            findNavController().navigate(
                DisaseDetailFragmentDirections.actionDisaseDetailFragmentToDisaseFragment()
            )
        }
        binding.btnDiseaseComplete.setOnClickListener {
            stopAlarm()
            deleteDisease()
        }
    }

    private fun showImage(){
        image = DisaseDetailFragmentArgs.fromBundle(requireArguments()).photoDisase.toString()
        binding.imgDisaseDetail.setImageURI(image.toUri())
    }

    private suspend fun insertDiseaseLocal(){
        delay(5000)
        val date = LocalDate.now().toString()

        val tempInsert = Disease(
            sortId,
            randomId,
            "test peyakit",
            binding.tvdisasaeDetailIndication.text.toString(),
            image,
            date,
            curLatitude,
            curLongitude,
            binding.tvdisasaeDetailDescription.text.toString(),
            alarmID,
            isUploaded = false
        )


        try {
            viewModel.insertDiseaseLocal(tempInsert)
            setReminder()
            showToast("Berhasil Disimpan")
            findNavController().navigate(
                DisaseDetailFragmentDirections.actionDisaseDetailFragmentToDisaseFragment()
            )
        }catch (e : Exception){
            showToast(e.message.toString())
        }
    }


    // Location Request
    private fun createLocationRequest(){
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(1)
            maxWaitTime = TimeUnit.SECONDS.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                lifecycleScope.launch {
                    getCurrentLocation()
                }
            }
            .addOnFailureListener {
                Log.d(page_key,it.message.toString())
            }

    }

    private suspend fun getCurrentLocation(){
        if (checkPermission(fineLocation) && checkPermission(coarseLocation)){
            fusedLocation.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null){
                        curLatitude = location.latitude
                        curLongitude = location.longitude
                        Log.d("disease",curLatitude.toString())
                        binding.tvdisasaeDetailLocation.visibility = View.VISIBLE
                    }

                }
                .addOnFailureListener {
                    Log.d(page_key,it.message.toString())
                }
        }else{
            requestPermissionLauncher.launch(arrayOf(
                fineLocation,
                coarseLocation
            ))
        }
    }

    private fun setReminder(){
        val time = Date()
        val currentTime = timeFormat.format(time)

        alarmReceive.setRepeatReminder(
            requireContext(),
            AlarmReceiver.type_alarm,
            currentTime,
            binding.tvdisasaeDetailIndication.text.toString(),
            alarmID
        )
    }

    private fun deleteDisease(){
        try {
            viewModel.deleteDiseaseLocal(randomId)
            Log.d("Diseasedetail",randomId)
            showToast("Penyakit telah teratasi")
            findNavController().navigate(
                DisaseDetailFragmentDirections.actionDisaseDetailFragmentToDisaseFragment()
            )
        }catch (e : Exception){
            showToast(e.message.toString())
        }

    }

    private fun stopAlarm(){
        alarmReceive.cancelAlarm(requireContext(),alarmID)
    }


    private fun showToast(title : String){
        Toast.makeText(requireContext(),title, Toast.LENGTH_SHORT).show()
    }

    companion object{
        const val page_key = "Disease_detail"
        val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    }


}