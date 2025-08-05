package com.liuxinyu.neurosleep.feature.stimulus.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.liuxinyu.neurosleep.R

/**
 * 蓝牙设备列表适配器
 */
class DeviceListAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {
    
    private var devices = listOf<BluetoothDevice>()
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateDevices(newDevices: List<BluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return DeviceViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }
    
    override fun getItemCount(): Int = devices.size
    
    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_device)
        private val deviceName: TextView = itemView.findViewById(R.id.tv_device_name)
        private val deviceAddress: TextView = itemView.findViewById(R.id.tv_device_address)
        
        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice) {
            deviceName.text = device.name ?: "未知设备"
            deviceAddress.text = device.address
            
            cardView.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }
}
