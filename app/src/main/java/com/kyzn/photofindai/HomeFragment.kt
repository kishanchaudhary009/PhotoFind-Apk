package com.kyzn.photofindai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kyzn.photofindai.data.ImageEmbedding
import com.kyzn.photofindai.onnx.ImageProcessor
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*

class HomeFragment : Fragment() {

    private lateinit var tvIndexedStats: TextView
    private lateinit var tvSyncHint: TextView
    private lateinit var pbIndexing: ProgressBar
    private lateinit var btnIndexNow: Button
    private lateinit var statusCard: View
    private lateinit var tvActiveStatus: TextView
    private lateinit var tvActivePercentage: TextView

    private var totalImagesInGallery = 0
    private var indexingJob: Job? = null
    private val TAG = "HomeFragment"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkGalleryStatus()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        tvIndexedStats = view.findViewById(R.id.tv_indexed_stats)
        tvSyncHint = view.findViewById(R.id.tv_sync_hint)
        pbIndexing = view.findViewById(R.id.pb_indexing)
        btnIndexNow = view.findViewById(R.id.btn_index_now)
        statusCard = view.findViewById(R.id.status_card)
        tvActiveStatus = view.findViewById(R.id.tv_active_status)
        tvActivePercentage = view.findViewById(R.id.tv_active_percentage)
        
        btnIndexNow.setOnClickListener {
            val app = requireActivity().application as ClipApp
            if (app.isIndexing) {
                stopIndexing()
            } else {
                startIndexing()
            }
        }

        view.findViewById<Button>(R.id.btn_search).setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_search
        }

        view.findViewById<View>(R.id.btn_about).setOnClickListener {
            showAboutDialog()
        }

        checkPermissionAndStatus()
        
        return view
    }

    override fun onResume() {
        super.onResume()
        checkGalleryStatus()
    }

    private fun checkPermissionAndStatus() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            checkGalleryStatus()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun checkGalleryStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scanner = GalleryScanner(requireContext())
            val allImages = scanner.getAllImagePaths().distinct()
            totalImagesInGallery = allImages.size
            val app = requireActivity().application as ClipApp
            val indexedCount = app.indexedEmbeddings.size
            
            withContext(Dispatchers.Main) {
                updateUI(indexedCount, totalImagesInGallery)
            }
        }
    }

    private fun updateUI(indexed: Int, total: Int) {
        tvIndexedStats.text = "$indexed / $total"
        pbIndexing.max = total
        pbIndexing.progress = indexed
        
        val remaining = total - indexed
        val app = requireActivity().application as ClipApp

        if (remaining > 0 || app.isIndexing) {
            tvSyncHint.text = if (remaining > 0) "$remaining new images found in gallery" else "Indexing in progress..."
            tvSyncHint.visibility = View.VISIBLE
            
            btnIndexNow.visibility = View.VISIBLE
            if (app.isIndexing) {
                btnIndexNow.text = "Pause Indexing"
                btnIndexNow.isEnabled = true
                statusCard.visibility = View.VISIBLE
            } else {
                btnIndexNow.text = "Start Indexing"
                btnIndexNow.isEnabled = true
                statusCard.visibility = View.GONE
            }
        } else {
            tvSyncHint.text = "Gallery fully indexed"
            btnIndexNow.visibility = View.GONE
            statusCard.visibility = View.GONE
        }
    }

    private fun stopIndexing() {
        indexingJob?.cancel()
        val app = requireActivity().application as ClipApp
        app.isIndexing = false
        updateUI(app.indexedEmbeddings.size, totalImagesInGallery)
    }

    private fun startIndexing() {
        val app = requireActivity().application as ClipApp
        if (app.isIndexing) return
        
        app.isIndexing = true
        updateUI(app.indexedEmbeddings.size, totalImagesInGallery)
        
        indexingJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                app.initML()
                
                val scanner = GalleryScanner(requireContext())
                val allImages = scanner.getAllImagePaths().distinct()
                
                val indexedPaths = synchronized(app.indexedEmbeddings) {
                    app.indexedEmbeddings.map { it.path }.toSet()
                }
                val toIndex = allImages.filter { it !in indexedPaths }
                
                if (toIndex.isEmpty()) return@launch

                toIndex.forEachIndexed { index, path ->
                    if (!isActive) return@forEachIndexed 
                    
                    try {
                        val bitmap = loadOptimizedBitmap(path)
                        if (bitmap != null) {
                            val preprocessed = ImageProcessor.preprocess(bitmap)
                            val embedding = app.clipModel.encodeImage(preprocessed, longArrayOf(1, 3, 224, 224))
                            if (embedding != null) {
                                synchronized(app.indexedEmbeddings) {
                                    if (app.indexedEmbeddings.none { it.path == path }) {
                                        app.indexedEmbeddings.add(ImageEmbedding(path, embedding))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing $path", e)
                    }
                    
                    if ((index + 1) % 20 == 0 || index == toIndex.size - 1) {
                        app.saveToDisk()
                    }
                    
                    withContext(Dispatchers.Main) {
                        val currentIndexed = synchronized(app.indexedEmbeddings) { app.indexedEmbeddings.size }
                        val progressPercent = if (totalImagesInGallery > 0) (currentIndexed * 100) / totalImagesInGallery else 100
                        
                        updateUI(currentIndexed, totalImagesInGallery)
                        tvActiveStatus.text = "Overall Progress: $currentIndexed / $totalImagesInGallery"
                        tvActivePercentage.text = "$progressPercent%"
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    app.isIndexing = false
                    statusCard.visibility = View.GONE
                    checkGalleryStatus()
                }
            }
        }
    }

    private fun showAboutDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_PhotoFindAi)
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        
        dialogView.findViewById<View>(R.id.btn_close_about).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun loadOptimizedBitmap(uriString: String): Bitmap? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            requireContext().contentResolver.openInputStream(uri).use { 
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, 224, 224)
            options.inJustDecodeBounds = false
            requireContext().contentResolver.openInputStream(uri).use { 
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
