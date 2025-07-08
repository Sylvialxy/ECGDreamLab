package com.liuxinyu.neurosleep

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private var devices: List<BluetoothDevice>, private val onConnectClickListener: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<DeviceAdapter.BleDeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleDeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ble_device, parent, false)
        return BleDeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: BleDeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
        holder.connectButton.setOnClickListener { onConnectClickListener(device) }
    }

    override fun getItemCount(): Int = devices.size

    class BleDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.device_name)
        val connectButton: Button = itemView.findViewById(R.id.connect_button)

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice) {
            deviceNameTextView.text = device.name ?: "Unknown Device"
        }
    }
}