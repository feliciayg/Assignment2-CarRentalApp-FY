package com.example.assignment2_carrentalapp_fy.model

import android.os.Parcel
import android.os.Parcelable
import java.text.NumberFormat
import java.util.Locale

data class Car(
    val id: String,
    val name: String,
    val model: String,
    val year: Int,
    val rating: Float,
    val kilometres: Int,
    val dailyCost: Int,
    val imageResId: Int,
    val isFavourite: Boolean = false,
    val isAvailable: Boolean = true
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        id = requireNotNull(parcel.readString()),
        name = requireNotNull(parcel.readString()),
        model = requireNotNull(parcel.readString()),
        year = parcel.readInt(),
        rating = parcel.readFloat(),
        kilometres = parcel.readInt(),
        dailyCost = parcel.readInt(),
        imageResId = parcel.readInt(),
        isFavourite = parcel.readByte() != 0.toByte(),
        isAvailable = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(model)
        parcel.writeInt(year)
        parcel.writeFloat(rating)
        parcel.writeInt(kilometres)
        parcel.writeInt(dailyCost)
        parcel.writeInt(imageResId)
        parcel.writeByte(if (isFavourite) 1 else 0)
        parcel.writeByte(if (isAvailable) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Car> {
        override fun createFromParcel(parcel: Parcel): Car = Car(parcel)

        override fun newArray(size: Int): Array<Car?> = arrayOfNulls(size)
    }
}

fun Car.nameWithModel(): String = "$name $model"

fun Car.metaLine(): String = "$model · $year · ${kilometres} km"

fun Car.formattedKilometres(): String = NumberFormat.getIntegerInstance().format(kilometres)

fun Car.formattedRating(): String = String.format(Locale.getDefault(), "%.1f", rating)
