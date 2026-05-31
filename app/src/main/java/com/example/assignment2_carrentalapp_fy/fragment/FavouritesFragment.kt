package com.example.assignment2_carrentalapp_fy.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment2_carrentalapp_fy.R
import com.example.assignment2_carrentalapp_fy.adapter.FavouriteAdapter
import com.example.assignment2_carrentalapp_fy.databinding.FragmentFavouritesBinding
import com.example.assignment2_carrentalapp_fy.model.Car

// Reusable home-screen section for favourite cars. It renders a list and sends
// selection/removal events back to MainActivity through Listener callbacks.
class FavouritesFragment : Fragment() {

    interface Listener {
        fun onFavouriteSelected(carId: String)
        fun onFavouriteRemoved(carId: String)
    }

    private var listener: Listener? = null
    private var binding: FragmentFavouritesBinding? = null
    private val favouriteAdapter by lazy {
        FavouriteAdapter(
            onCarSelected = { listener?.onFavouriteSelected(it.id) },
            onFavouriteToggled = { listener?.onFavouriteRemoved(it.id) }
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Listener avoids the fragment directly depending on MainActivity implementation details.
        listener = context as? Listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavouritesBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.recyclerFavourites?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerFavourites?.adapter = favouriteAdapter
    }

    fun submitFavourites(cars: List<Car>) {
        // Update list content, title count, and empty state from the same source.
        favouriteAdapter.submitList(cars)
        binding?.textFavouritesTitle?.text = getString(R.string.favourites_title_count_format, cars.size)
        binding?.textEmptyFavourites?.visibility = if (cars.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
