package com.example.rifsa_mobile.view.fragment.inventory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rifsa_mobile.R
import com.example.rifsa_mobile.databinding.FragmentInventoryBinding
import com.example.rifsa_mobile.model.entity.remotefirebase.InventoryFirebaseEntity
import com.example.rifsa_mobile.view.fragment.inventory.adapter.InventoryRecyclerViewAdapter
import com.example.rifsa_mobile.viewmodel.RemoteViewModel
import com.example.rifsa_mobile.viewmodel.UserPrefrencesViewModel
import com.example.rifsa_mobile.viewmodel.utils.ViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class InventoryFragment : Fragment() {
    private lateinit var binding : FragmentInventoryBinding

    private val remoteViewModel : RemoteViewModel by viewModels{ ViewModelFactory.getInstance(requireContext()) }
    private val authViewModel : UserPrefrencesViewModel by viewModels { ViewModelFactory.getInstance(requireContext()) }

    private var dataList = ArrayList<InventoryFirebaseEntity>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInventoryBinding.inflate(layoutInflater)

        val bottomMenu = requireActivity().findViewById<BottomNavigationView>(R.id.main_bottommenu)
        bottomMenu.visibility = View.VISIBLE

        binding.fabInventoryAdd.setOnClickListener {
            findNavController().navigate(
                InventoryFragmentDirections.actionInventoryFragmentToInvetoryInsertFragment(null)
            )
        }

        authViewModel.getUserId().observe(viewLifecycleOwner){ token->
            inventoryList(token)
        }


        return binding.root
    }

    private fun inventoryList(token : String){
        remoteViewModel.readInventory(token).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.children.forEach { main ->
                        binding.pgbInventoryBar.visibility = View.GONE
                        val data = main.getValue(InventoryFirebaseEntity::class.java)
                        data?.let { dataList.add(data) }
                        showInventoryList(dataList)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showStatus(error.message)
            }
        })
    }

    private fun showInventoryList(data : List<InventoryFirebaseEntity>) {
        try {
            val adapter = InventoryRecyclerViewAdapter(data)
            val recyclerView = binding.recviewInventory
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            adapter.onItemDetailCallback(object : InventoryRecyclerViewAdapter.OnDetailItemCallback{
                override fun onDetailCallback(data: InventoryFirebaseEntity) {
                    findNavController().navigate(
                            InventoryFragmentDirections.actionInventoryFragmentToInvetoryInsertFragment(data)
                    )
                }
            })
        }catch (e : Exception){
            showStatus(e.message.toString())
        }
    }

    private fun showStatus(title : String){
        binding.pgbInventoryStatus.text = title
        binding.pgbInventoryStatus.visibility = View.VISIBLE

        if (title.isNotEmpty()){
            binding.pgbInventoryBar.visibility = View.GONE
        }

        Log.d("InventoryFragment",title)
    }

}