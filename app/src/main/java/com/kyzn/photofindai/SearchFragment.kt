package com.kyzn.photofindai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.kyzn.photofindai.data.ImageEmbedding
import com.kyzn.photofindai.onnx.ImageProcessor
import com.kyzn.photofindai.onnx.VectorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var etSearchText: EditText
    private lateinit var btnSearchSubmit: Button
    private lateinit var btnCamera: LinearLayout
    private lateinit var btnGallery: LinearLayout
    private lateinit var spinnerLimit: Spinner
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var adapter: SearchResultAdapter
    private val searchResults = mutableListOf<ImageEmbedding>()

    // Selected Image UI elements
    private lateinit var clSelectedImage: View
    private lateinit var ivSelectedPreview: ImageView
    private lateinit var btnRemoveImage: ImageButton
    private lateinit var llImageSearch: LinearLayout
    private lateinit var tvOr: TextView
    private var selectedBitmap: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            updateSearchUI(true)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    updateSearchUI(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Detail Overlay Views
    private lateinit var detailOverlay: View
    private lateinit var ivDetailImage: ImageView
    private lateinit var btnCloseDetail: ImageButton
    private lateinit var btnGalleryOpen: ImageButton
    private lateinit var btnShareDetail: ImageButton
    private lateinit var btnSearchSimilar: ImageButton
    private lateinit var tvImageInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        
        etSearchText = view.findViewById(R.id.et_search_text)
        btnSearchSubmit = view.findViewById(R.id.btn_search_submit)
        btnCamera = view.findViewById(R.id.btn_camera)
        btnGallery = view.findViewById(R.id.btn_gallery)
        spinnerLimit = view.findViewById(R.id.spinner_limit)
        rvSearchResults = view.findViewById(R.id.rv_search_results)

        // Selected Image UI
        clSelectedImage = view.findViewById(R.id.cl_selected_image)
        ivSelectedPreview = view.findViewById(R.id.iv_selected_preview)
        btnRemoveImage = view.findViewById(R.id.btn_remove_image)
        llImageSearch = view.findViewById(R.id.ll_image_search)
        tvOr = view.findViewById(R.id.tv_or)
        
        // Find detail overlay views (assumed to be included in fragment_search.xml or handled via a dialog/overlay)
        detailOverlay = view.findViewById(R.id.detail_overlay)
        ivDetailImage = view.findViewById(R.id.iv_detail_image)
        btnCloseDetail = view.findViewById(R.id.btn_close)
        btnGalleryOpen = view.findViewById(R.id.btn_gallery_open)
        btnShareDetail = view.findViewById(R.id.btn_share_detail)
        btnSearchSimilar = view.findViewById(R.id.btn_search_similar)
        tvImageInfo = view.findViewById(R.id.tv_image_info)
        
        adapter = SearchResultAdapter(searchResults) { selectedImage ->
            showImageDetail(selectedImage)
        }
        rvSearchResults.adapter = adapter
        
        btnSearchSubmit.setOnClickListener {
            if (selectedBitmap != null) {
                performImageSearch(selectedBitmap!!)
            } else {
                val query = etSearchText.text.toString()
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    Toast.makeText(context, "Please enter text or select an image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCamera.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemoveImage.setOnClickListener {
            selectedBitmap = null
            updateSearchUI(false)
        }

        btnCloseDetail.setOnClickListener {
            detailOverlay.visibility = View.GONE
        }
        
        return view
    }

    private fun showImageDetail(item: ImageEmbedding) {
        detailOverlay.visibility = View.VISIBLE
        try {
            val uri = Uri.parse(item.path)
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            ivDetailImage.setImageBitmap(bitmap)
            
            val mimeType = requireContext().contentResolver.getType(uri) ?: "image/*"
            val extension = mimeType.substringAfter('/').uppercase()
            tvImageInfo.text = "Image • ${bitmap?.width ?: 0}x${bitmap?.height ?: 0} • $extension"

            btnGalleryOpen.setOnClickListener {
                openInGallery(uri)
            }

            btnShareDetail.setOnClickListener {
                shareImage(uri)
            }

            btnSearchSimilar.setOnClickListener {
                detailOverlay.visibility = View.GONE
                try {
                    val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(uri))
                    if (bitmap != null) {
                        selectedBitmap = bitmap
                        updateSearchUI(true)
                        performImageSearch(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openInGallery(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Open with"))
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePictureLauncher.launch(null)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun updateSearchUI(imageSelected: Boolean) {
        if (imageSelected) {
            etSearchText.visibility = View.GONE
            tvOr.visibility = View.GONE
            llImageSearch.visibility = View.GONE
            clSelectedImage.visibility = View.VISIBLE
            ivSelectedPreview.setImageBitmap(selectedBitmap)
        } else {
            etSearchText.visibility = View.VISIBLE
            tvOr.visibility = View.VISIBLE
            llImageSearch.visibility = View.VISIBLE
            clSelectedImage.visibility = View.GONE
            ivSelectedPreview.setImageBitmap(null)
        }
    }

    private fun shareImage(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share Image"))
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val app = requireActivity().application as ClipApp
            app.initML()
            
            val queryTokens = app.clipTokenizer.tokenize(query)
            val queryEmbedding = app.clipModel.encodeText(queryTokens)
            
            if (queryEmbedding != null) {
                val limit = spinnerLimit.selectedItem.toString().toIntOrNull() ?: 10
                
                val results = app.indexedEmbeddings.map { 
                    it to VectorUtils.cosineSimilarity(queryEmbedding, it.embedding)
                }.sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
                
                withContext(Dispatchers.Main) {
                    searchResults.clear()
                    searchResults.addAll(results)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun performImageSearch(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val app = requireActivity().application as ClipApp
            app.initML()

            val imageBuffer = ImageProcessor.preprocess(bitmap)
            val queryEmbedding = app.clipModel.encodeImage(imageBuffer, longArrayOf(1, 3, 224, 224))

            if (queryEmbedding != null) {
                val limit = spinnerLimit.selectedItem.toString().toIntOrNull() ?: 10

                val results = app.indexedEmbeddings.map {
                    it to VectorUtils.cosineSimilarity(queryEmbedding, it.embedding)
                }.sortedByDescending { it.second }
                    .take(limit)
                    .map { it.first }

                withContext(Dispatchers.Main) {
                    searchResults.clear()
                    searchResults.addAll(results)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    class SearchResultAdapter(
        private val items: List<ImageEmbedding>,
        private val onItemClick: (ImageEmbedding) -> Unit
    ) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivResult: ImageView = view.findViewById(R.id.iv_result)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.itemView.setOnClickListener { onItemClick(item) }
            try {
                val context = holder.itemView.context
                val uri = Uri.parse(item.path)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                holder.ivResult.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivResult.setImageResource(R.drawable.ic_placeholder_image)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
