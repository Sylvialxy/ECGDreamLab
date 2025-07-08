package com.liuxinyu.neurosleep.feature.home.experiment

class ExperimentAdapter /*: ListAdapter<ExperimentVO, ExperimentAdapter.ViewHolder>(DIFF_CALLBACK) {
    inner class ViewHolder(val binding: ItemExperimentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExperimentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val experiment = getItem(position)
        holder.binding.tvExperimentName.text = experiment.name
        holder.binding.tvExperimentDescription.text = experiment.description
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ExperimentVO>() {
            override fun areItemsTheSame(oldItem: ExperimentVO, newItem: ExperimentVO): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ExperimentVO, newItem: ExperimentVO): Boolean {
                return oldItem == newItem
            }
        }
    }
}*/