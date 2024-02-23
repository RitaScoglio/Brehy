package sk.brehy.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import sk.brehy.R

data class People(var number: String, var name: String)

class PeopleAdapter(context: Context, words: List<People>)
    : ArrayAdapter<People>(context, 0, words as MutableList<People>) {
    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = LayoutInflater.from(context).inflate(R.layout.people_list, parent, false)
        val current = getItem(position)
        val number = listItemView.findViewById<TextView>(R.id.number)
        number.text = current!!.number
        val name = listItemView.findViewById<TextView>(R.id.name)
        name.text = current.name
        return listItemView
    }
}