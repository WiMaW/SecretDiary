package org.hyperskill.secretdiary

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.appcompat.app.AppCompatActivity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.hyperskill.secretdiary.databinding.ActivityMainBinding
import java.time.format.DateTimeFormatter

const val PREFERENCES_NAME = "PREF_DIARY"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val messagesSet: MutableSet<Message> = mutableSetOf()
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        getMessagesFromSharedPref(sharedPreferences, messagesSet)
        refreshDiaryTexts(binding, messagesSet)

        binding.btnSave.setOnClickListener {
            if (binding.etNewWriting.text.isBlank() || binding.etNewWriting.text.isEmpty()) {
                Toast.makeText(this, "Empty or blank input cannot be saved", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val currentMessages = sharedPreferences.getString("KEY_DIARY_TEXT", null)

                val newMessage = Message(
                    text = binding.etNewWriting.text.toString(),
                    date = convertDateAndTimeToString()
                )

                val updatedMessages = if (currentMessages.isNullOrBlank()) {
                    "${newMessage.date}\n${newMessage.text}"
                } else {
                    "${newMessage.date}\n${newMessage.text}\n\n$currentMessages"
                }

                editor.putString(
                    "KEY_DIARY_TEXT",
                    updatedMessages
                ).apply()

                getMessagesFromSharedPref(sharedPreferences, messagesSet)
                refreshDiaryTexts(binding, messagesSet)

            }
            binding.etNewWriting.text.clear()
        }

        binding.btnUndo.setOnClickListener {
            alertDialog(this, editor, sharedPreferences, messagesSet, binding)
        }

        /*
            Tests for android can not guarantee the correctness of solutions that make use of
            mutation on "static" variables to keep state. You should avoid using those.
            Consider "static" as being anything on kotlin that is transpiled to java
            into a static variable. That includes global variables and variables inside
            singletons declared with keyword object, including companion object.
            This limitation is related to the use of JUnit on tests. JUnit re-instantiate all
            instance variable for each test method, but it does not re-instantiate static variables.
            The use of static variable to hold state can lead to state from one test to spill over
            to another test and cause unexpected results.
            Using mutation on static variables to keep state
            is considered a bad practice anyway and no measure
            attempting to give support to that pattern will be made.
         */


    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        getMessagesFromSharedPref(sharedPreferences, messagesSet)
        refreshDiaryTexts(binding, messagesSet)
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun alertDialog(
    context: Context,
    editor: Editor,
    sharedPreferences: SharedPreferences,
    messageSet: MutableSet<Message>,
    binding: ActivityMainBinding,
) {
    AlertDialog.Builder(context)
        .setTitle("Remove last note")
        .setMessage("Do you really want to remove the last writing? This operation cannot be undone!")
        .setPositiveButton("Yes") { _, _ ->
            deleteMessageFromSharedPref(editor, sharedPreferences, messageSet, binding)
        }
        .setNegativeButton("No", null)
        .show()
}

fun getMessagesFromSharedPref(
    sharedPreferences: SharedPreferences,
    messageSet: MutableSet<Message>
) {
    val messagesInSharedPref: MutableList<String> = mutableListOf()

    sharedPreferences.getString("KEY_DIARY_TEXT", null)
        ?.let { storedText -> if (!storedText.isNullOrBlank()) messagesInSharedPref.addAll(storedText.split("\n\n")) }

    messageSet.clear()

    if (messagesInSharedPref.isNotEmpty()) {
        for (messageString in messagesInSharedPref) {
            val parts = messageString.split("\n")
            if (parts.size >= 2) {
                val newMessage = Message(
                    date = parts[0],
                    text = parts[1]
                )
                messageSet.add(newMessage)
            }
        }
    } else {
        messageSet.clear()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun deleteMessageFromSharedPref(
    editor: Editor,
    sharedPreferences: SharedPreferences,
    messageSet: MutableSet<Message>,
    binding: ActivityMainBinding
) {
    val messagesInSharedPref: MutableList<String> = mutableListOf()

    sharedPreferences.getString("KEY_DIARY_TEXT", null)
        ?.let { storedText -> if (!storedText.isNullOrBlank()) messagesInSharedPref.addAll(storedText.split("\n\n")) }

    if (messagesInSharedPref.isNotEmpty()) {
        messagesInSharedPref.removeAt(0)
    }
    editor.putString(
        "KEY_DIARY_TEXT",
        messagesInSharedPref.joinToString("\n")
    ).apply()

    messageSet.clear()
    getMessagesFromSharedPref(sharedPreferences, messageSet)
    refreshDiaryTexts(binding, messageSet)
}

@RequiresApi(Build.VERSION_CODES.O)
fun refreshDiaryTexts(binding: ActivityMainBinding, messageSet: MutableSet<Message>) {
    binding.tvDiary.text = messageSet.joinToString(separator = "\n\n"){ message ->
        "${message.date}\n${message.text}"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun convertDateAndTimeToString(): String {
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val dateString = "${date.date} ${date.toJavaLocalDateTime().format(timeFormatter)}"
    return dateString
}

