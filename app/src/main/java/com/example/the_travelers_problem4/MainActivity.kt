package com.example.the_travelers_problem4

import android.graphics.*
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    // Modelos
    data class City(val name: String, val id: Int)
    
    data class Node(
        val currentCity: Int,
        val visitedMask: Int,
        val g: Double,
        val h: Double,
        val path: List<Int>
    ) : Comparable<Node> {
        val f: Double get() = g + h
        override fun compareTo(other: Node): Int = f.compareTo(other.f)
    }

    // Datos del estado
    private val citiesList = mutableListOf<City>()
    private var distanceMatrix = Array(0) { DoubleArray(0) }
    
    // Estadísticas para el Profe
    private var expandedNodes = 0
    private var generatedNodes = 0
    private var prunedNodes = 0

    // UI Components
    private lateinit var cardStep1: CardView
    private lateinit var cardStep2: CardView
    private lateinit var cardResults: CardView
    private lateinit var chipGroupCities: ChipGroup
    private lateinit var etCityName: EditText
    private lateinit var layoutDistanceInputs: LinearLayout
    private lateinit var ivGraph: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        setupUI()
    }

    private fun setupUI() {
        cardStep1 = findViewById(R.id.cardStep1)
        cardStep2 = findViewById(R.id.cardStep2)
        cardResults = findViewById(R.id.cardResults)
        chipGroupCities = findViewById(R.id.chipGroupCities)
        etCityName = findViewById(R.id.etCityName)
        layoutDistanceInputs = findViewById(R.id.layoutDistanceInputs)
        ivGraph = findViewById(R.id.ivGraph)

        val btnAddCity = findViewById<Button>(R.id.btnAddCity)
        val btnNextToDistances = findViewById<Button>(R.id.btnNextToDistances)
        val btnSolve = findViewById<Button>(R.id.btnSolve)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val tvCitiesCount = findViewById<TextView>(R.id.tvCitiesCount)

        btnAddCity.setOnClickListener {
            val name = etCityName.text.toString().trim()
            if (name.isNotEmpty() && citiesList.size < 8) {
                addCity(name)
                etCityName.text.clear()
                tvCitiesCount.text = "${citiesList.size} / 8 ciudades"
                btnNextToDistances.isEnabled = citiesList.size >= 2
            }
        }

        btnNextToDistances.setOnClickListener {
            showStep2()
        }

        btnSolve.setOnClickListener {
            if (captureDistances()) {
                solveAndShowResults()
            }
        }

        btnReset.setOnClickListener {
            resetAll()
        }
    }

    private fun addCity(name: String) {
        val city = City(name, citiesList.size)
        citiesList.add(city)
        val chip = Chip(this).apply {
            text = name
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                citiesList.remove(city)
                chipGroupCities.removeView(this)
                findViewById<TextView>(R.id.tvCitiesCount).text = "${citiesList.size} / 8 ciudades"
                findViewById<Button>(R.id.btnNextToDistances).isEnabled = citiesList.size >= 2
            }
        }
        chipGroupCities.addView(chip)
    }

    private fun showStep2() {
        cardStep1.visibility = View.GONE
        cardStep2.visibility = View.VISIBLE
        layoutDistanceInputs.removeAllViews()

        distanceMatrix = Array(citiesList.size) { DoubleArray(citiesList.size) }

        // Crear inputs para cada par de ciudades (Triángulo superior)
        for (i in citiesList.indices) {
            for (j in i + 1 until citiesList.size) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                }
                val label = TextView(this).apply {
                    text = "De ${citiesList[i].name} a ${citiesList[j].name}:"
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                }
                val input = EditText(this).apply {
                    hint = "KM"
                    inputType = InputType.TYPE_CLASS_NUMBER
                    tag = "dist_${i}_${j}"
                    layoutParams = LinearLayout.LayoutParams(200, -2)
                }
                row.addView(label)
                row.addView(input)
                layoutDistanceInputs.addView(row)
            }
        }
    }

    private fun captureDistances(): Boolean {
        try {
            for (i in citiesList.indices) {
                for (j in i + 1 until citiesList.size) {
                    val input = layoutDistanceInputs.findViewWithTag<EditText>("dist_${i}_${j}")
                    val value = input?.text.toString().toDoubleOrNull() ?: 0.0
                    distanceMatrix[i][j] = value
                    distanceMatrix[j][i] = value
                }
            }
            return true
        } catch (e: Exception) {
            Toast.makeText(this, "Ingresa valores válidos", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun solveAndShowResults() {
        // Reset stats
        expandedNodes = 0
        generatedNodes = 0
        prunedNodes = 0

        val bestNode = runAStar()

        if (bestNode != null) {
            cardStep2.visibility = View.GONE
            cardResults.visibility = View.VISIBLE

            findViewById<TextView>(R.id.tvBestPath).text = 
                bestNode.path.joinToString(" → ") { citiesList[it].name }
            
            findViewById<TextView>(R.id.tvTotalDistance).text = 
                "Distancia óptima: ${bestNode.g.toInt()} km"

            findViewById<TextView>(R.id.tvStats).text = 
                "Nodos expandidos: $expandedNodes\n" +
                "Nodos generados: $generatedNodes\n" +
                "Nodos podados: $prunedNodes"

            drawGraph(bestNode.path)
        }
    }

    private fun runAStar(): Node? {
        val n = citiesList.size
        val allVisited = (1 shl n) - 1
        val pq = PriorityQueue<Node>()
        
        // El estado es (currentCity, visitedMask)
        val minCostToState = mutableMapOf<Pair<Int, Int>, Double>()

        // Nodo inicial (Ciudad 0)
        val startNode = Node(0, 1 shl 0, 0.0, estimateH(0, 1 shl 0), listOf(0))
        pq.add(startNode)
        generatedNodes++

        while (pq.isNotEmpty()) {
            val current = pq.poll()!!
            expandedNodes++

            // Si ya encontramos un camino mejor a este mismo estado, podamos
            val state = Pair(current.currentCity, current.visitedMask)
            if (minCostToState.containsKey(state) && minCostToState[state]!! < current.g) {
                prunedNodes++
                continue
            }
            minCostToState[state] = current.g

            if (current.visitedMask == allVisited) {
                // Volver al origen
                val finalG = current.g + distanceMatrix[current.currentCity][0]
                return Node(0, allVisited, finalG, 0.0, current.path + 0)
            }

            for (next in 0 until n) {
                if ((current.visitedMask and (1 shl next)) == 0) {
                    val newG = current.g + distanceMatrix[current.currentCity][next]
                    val nextNode = Node(
                        next, 
                        current.visitedMask or (1 shl next),
                        newG,
                        estimateH(next, current.visitedMask or (1 shl next)),
                        current.path + next
                    )
                    
                    generatedNodes++
                    // Poda básica: si f(n) ya es peor que algo que conocemos, o similar
                    pq.add(nextNode)
                }
            }
        }
        return null
    }

    private fun estimateH(current: Int, mask: Int): Double {
        // Heurística simple: distancia mínima a alguna ciudad no visitada + distancia mínima de alguna no visitada al origen
        var h = 0.0
        val n = citiesList.size
        var minToUnvisited = Double.MAX_VALUE
        var anyUnvisited = false

        for (i in 0 until n) {
            if ((mask and (1 shl i)) == 0) {
                anyUnvisited = true
                if (distanceMatrix[current][i] < minToUnvisited) {
                    minToUnvisited = distanceMatrix[current][i]
                }
            }
        }
        if (anyUnvisited) h += minToUnvisited
        return h
    }

    private fun drawGraph(path: List<Int>) {
        val size = 1000
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        canvas.drawColor(Color.WHITE)

        // Calcular posiciones de ciudades en círculo
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size * 0.35f
        val n = citiesList.size
        val positions = Array(n) { i ->
            val angle = Math.toRadians((i * 360.0 / n) - 90)
            PointF(
                centerX + radius * cos(angle).toFloat(),
                centerY + radius * sin(angle).toFloat()
            )
        }

        // Dibujar Conexiones de la Ruta
        paint.color = Color.parseColor("#1976D2")
        paint.strokeWidth = 5f
        for (i in 0 until path.size - 1) {
            val p1 = positions[path[i]]
            val p2 = positions[path[i+1]]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
            
            // Dibujar distancia en el medio de la línea
            paint.color = Color.DKGRAY
            paint.textSize = 30f
            val distText = "${distanceMatrix[path[i]][path[i+1]].toInt()} km"
            canvas.drawText(distText, (p1.x + p2.x)/2, (p1.y + p2.y)/2, paint)
            paint.color = Color.parseColor("#1976D2")
        }

        // Dibujar Nodos
        for (i in 0 until n) {
            paint.color = if (i == 0) Color.parseColor("#D32F2F") else Color.parseColor("#00796B")
            canvas.drawCircle(positions[i].x, positions[i].y, 40f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 35f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(citiesList[i].name.take(2), positions[i].x, positions[i].y + 12f, paint)
            
            // Nombre debajo
            paint.color = Color.BLACK
            canvas.drawText(citiesList[i].name, positions[i].x, positions[i].y + 80f, paint)
        }

        ivGraph.setImageBitmap(bitmap)
    }

    private fun resetAll() {
        citiesList.clear()
        chipGroupCities.removeAllViews()
        findViewById<TextView>(R.id.tvCitiesCount).text = "0 / 8 ciudades"
        findViewById<Button>(R.id.btnNextToDistances).isEnabled = false
        cardStep1.visibility = View.VISIBLE
        cardStep2.visibility = View.GONE
        cardResults.visibility = View.GONE
    }
}
