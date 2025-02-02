package com.example.chatfirebase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatfirebase.adapter.ChatAdapter
import com.example.chatfirebase.databinding.ActivityChatBinding
import com.example.chatfirebase.model.ChatModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    var emailUsuarioLogeado = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private var listadoChats = mutableListOf<ChatModel>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para acceder al chat", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailUsuarioLogeado = auth.currentUser?.email.toString()

        val firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseDatabase.setPersistenceEnabled(true)

        databaseRef = FirebaseDatabase.getInstance().getReference("chat")

        setRecycler()
        setListeners()
    }

    //----------------------------------------------------------------------------------------------
    private fun setRecycler() {
        val layoutManager = LinearLayoutManager(this)
        binding.rvChats.layoutManager = layoutManager

        adapter = ChatAdapter(listadoChats, emailUsuarioLogeado)
        binding.rvChats.adapter = adapter
    }

    //----------------------------------------------------------------------------------------------
    private fun setListeners() {
        binding.imageView.setOnClickListener {
            enviar()
        }
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listadoChats.clear()
                for (nodo in snapshot.children) {
                    val chatNodo = nodo.getValue(ChatModel::class.java)
                    if (chatNodo != null) {
                        listadoChats.add(chatNodo)
                    }
                }
                listadoChats.sortBy { it.fecha }
                adapter.updateAdapter(listadoChats)
                binding.rvChats.scrollToPosition(listadoChats.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ChatActivity,
                    "Error al recuperar los chats",
                    Toast.LENGTH_SHORT
                ).show()
            }

        })

        binding.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.item_salir -> {
                    finishAffinity()
                    true
                }
                R.id.item_logout -> {
                    auth.signOut()
                    startActivity(Intent(this@ChatActivity, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.item_borrar -> {
                    borrarChatUsuario()
                    true
                }
                else -> false
            }
        }

        binding.etMensaje.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                enviar()
                ocultarTeclado()
                true
            } else {
                false
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    private fun borrarChatUsuario() {
        databaseRef.get().addOnSuccessListener {
            for (nodo in it.children) {
                val correo = nodo.child("email").getValue(String::class.java)
                if (correo == emailUsuarioLogeado) {
                    nodo.ref.removeValue()
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@ChatActivity, "Error al borrar el mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
    //----------------------------------------------------------------------------------------------

    private fun enviar() {
        val usuario = auth.currentUser
        if (usuario == null) {
            Toast.makeText(this, "No estás autenticado, por favor inicia sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val texto = binding.etMensaje.text.toString().trim()
        if (texto.isEmpty()) {
            Toast.makeText(this, "No puedes enviar un mensaje vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val fecha = System.currentTimeMillis()
        val mensaje = ChatModel(usuario.email ?: "", texto, fecha)
        val key = fecha.toString()

        databaseRef.child(key).setValue(mensaje)
            .addOnSuccessListener {
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "No se pudo guardar el mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        binding.etMensaje.setText("")
    }

    //----------------------------------------------------------------------------------------------
    private fun ocultarTeclado() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}