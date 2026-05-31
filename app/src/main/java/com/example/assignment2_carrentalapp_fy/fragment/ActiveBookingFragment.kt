package com.example.assignment2_carrentalapp_fy.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.assignment2_carrentalapp_fy.databinding.FragmentActiveBookingBinding
import com.example.assignment2_carrentalapp_fy.model.Booking

// Reusable home-screen section for showing the current booking summary.
// MainActivity owns the action logic; this fragment only renders and reports clicks.
class ActiveBookingFragment : Fragment() {

    interface Listener {
        fun onEndRentalClicked()
    }

    private var listener: Listener? = null
    private var binding: FragmentActiveBookingBinding? = null
    private var currentBooking: Booking? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Parent activity implements the callback so the fragment stays reusable.
        listener = context as? Listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentActiveBookingBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.buttonEndRental?.setOnClickListener {
            listener?.onEndRentalClicked()
        }
        render()
    }

    fun bindBooking(booking: Booking?) {
        // Called by MainActivity whenever repository state changes.
        currentBooking = booking
        render()
    }

    private fun render() {
        val currentBinding = binding ?: return
        val booking = currentBooking
        currentBinding.textActiveBookingSummary.text = if (booking == null) {
            ""
        } else {
            "${booking.carName} ${booking.carModel} · ${booking.rentalDays} days · ${booking.totalCost} credits"
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
