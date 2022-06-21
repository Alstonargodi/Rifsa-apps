package com.example.rifsa_mobile.view.fragment.harvestresult

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rifsa_mobile.R
import com.example.rifsa_mobile.databinding.FragmentHarvetResultBinding
import com.example.rifsa_mobile.model.entity.remote.harvestresult.HarvestResponData
import com.example.rifsa_mobile.model.entity.remotefirebase.HarvestFirebaseEntity
import com.example.rifsa_mobile.utils.FetchResult
import com.example.rifsa_mobile.utils.Utils
import com.example.rifsa_mobile.view.fragment.harvestresult.adapter.HarvestResultRecyclerViewAdapter
import com.example.rifsa_mobile.viewmodel.RemoteViewModel
import com.example.rifsa_mobile.viewmodel.UserPrefrencesViewModel
import com.example.rifsa_mobile.viewmodel.utils.ViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch


class HarvetResultFragment : Fragment() {
    private lateinit var binding : FragmentHarvetResultBinding

    private val remoteViewModel : RemoteViewModel by viewModels{ ViewModelFactory.getInstance(requireContext()) }
    private val authViewModel : UserPrefrencesViewModel by viewModels { ViewModelFactory.getInstance(requireContext()) }

    private var isConnected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHarvetResultBinding.inflate(layoutInflater)
        isConnected = Utils.internetChecker(requireContext())


        val bottomMenu = requireActivity().findViewById<BottomNavigationView>(R.id.main_bottommenu)
        bottomMenu.visibility = View.VISIBLE



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fabHarvestToinsert.setOnClickListener {
            findNavController().navigate(
                HarvetResultFragmentDirections.actionHarvetResultFragmentToHarvestInsertDetailFragment(null)
            )
        }

        authViewModel.getUserToken().observe(viewLifecycleOwner){ token->
            binding.pgbHasilBar.visibility = View.VISIBLE
            remoteViewModel.readHarvestResult(token).addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        child.children.forEach { main ->
                            val data = main.getValue(HarvestFirebaseEntity::class.java)
                            val dataList = ArrayList<HarvestFirebaseEntity>()
                            data?.let { dataList.add(data) }
                            binding.pgbHasilBar.visibility = View.GONE
                            showResult(dataList)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.pgbHasilBar.visibility = View.GONE
                    showStatus(error.message)
                }
            })
        }



        binding.btnHarvestBackhome.setOnClickListener {
            findNavController().navigate(
                HarvetResultFragmentDirections.actionHarvetResultFragmentToHomeFragment()
            )
        }

    }

    private fun getResultFromRemote(token : String){
        lifecycleScope.launch {
            remoteViewModel.getHarvestRemote(token).observe(viewLifecycleOwner){
                when(it){
                    is FetchResult.Loading->{
                        binding.pgbHasilBar.visibility = View.VISIBLE
                    }
                    is FetchResult.Success->{

                        binding.pgbHasilBar.visibility = View.GONE
                    }
                    is FetchResult.Error ->{
                        showStatus(it.error)
                    }
                }
            }
        }
    }

    private fun showResult(data : List<HarvestFirebaseEntity>){
        val adapter = HarvestResultRecyclerViewAdapter(data)
        val recyclerView = binding.rvHarvestresult
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter.onDetailCallBack(object : HarvestResultRecyclerViewAdapter.OnDetailCallback{
            override fun onDetailCallback(data: HarvestResponData) {
                findNavController().navigate(HarvetResultFragmentDirections
                    .actionHarvetResultFragmentToHarvestInsertDetailFragment(data))
            }
        })
    }

    private fun showStatus(title: String){
        binding.pgbHasilTitle.text = title
        binding.pgbHasilTitle.visibility = View.VISIBLE

        if (title.isNotEmpty()){
            binding.pgbHasilBar.visibility = View.GONE
        }
    }



}