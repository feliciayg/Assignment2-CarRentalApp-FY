package com.example.assignment2_carrentalapp_fy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment2_carrentalapp_fy.R
import com.example.assignment2_carrentalapp_fy.databinding.ItemFavouriteBinding
import com.example.assignment2_carrentalapp_fy.model.Car
import com.example.assignment2_carrentalapp_fy.model.nameWithModel

// RecyclerView adapter for the compact Favourites section on the home screen.
class FavouriteAdapter(
    private val onCarSelected: (Car) -> Unit,
    private val onFavouriteToggled: (Car) -> Unit
) : ListAdapter<Car, FavouriteAdapter.FavouriteViewHolder>(FavouriteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemFavouriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavouriteViewHolder(
        private val binding: ItemFavouriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            binding.imageFavouriteCar.setImageResource(car.imageResId)
            binding.textFavouriteName.text = car.nameWithModel()
            binding.textFavouriteCost.text = binding.root.context.getString(
                R.string.daily_cost_format,
                car.dailyCost
            )
            binding.buttonRemoveFavourite.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.buttonRemoveFavourite.imageTintList = null

            binding.root.setOnClickListener { onCarSelected(car) }
            binding.buttonRemoveFavourite.setOnClickListener { onFavouriteToggled(car) }
        }
    }

    private object FavouriteDiffCallback : DiffUtil.ItemCallback<Car>() {
        // DiffUtil updates only rows that changed, keeping the favourites list efficient.
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean = oldItem == newItem
    }
}
