package com.idenc.snapceit

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), PersonSelectorDialogFragment.MyDialogListener {
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_IMAGE = 2
    private val PRICE_REGEX = Regex("([\\dO]+\\.\\d{1,2})")

    private lateinit var currentPhotoPath: Uri
    private lateinit var fragmentAdapter: RecyclerAdapter
    private var itemsList = ArrayList<Pair<String, String>>()
    private var people = ArrayList<Person>()
    private var currentAssignPosition: Int = -1

    //boolean flag to know if main FAB is in open or closed state.
    private var fabExpanded = false
    private lateinit var fabSettings: FloatingActionButton

    //Linear layout holding the Save submenu
    private lateinit var layoutFabAddItem: LinearLayout
    private lateinit var layoutFabConfirmItem: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        val navigation = view.findViewById<BottomNavigationView>(R.id.nav_view)

        // Add receipt through picture
        view.findViewById<BottomNavigationItemView>(R.id.cameraButton).setOnClickListener {
            navigation.selectedItemId = R.id.cameraButton
            dispatchTakePictureIntent()
        }

        // Add receipt through gallery
        view.findViewById<BottomNavigationItemView>(R.id.galleryButton).setOnClickListener {
            navigation.selectedItemId = R.id.galleryButton
            dispatchGalleryIntent()
        }

        initNavigation(navigation)
        // Make recycler view to hold parsed items
        fragmentAdapter = RecyclerAdapter(itemsList)
        view.findViewById<RecyclerView>(R.id.recycler).apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = fragmentAdapter
        }

        // Instantiate our current people
        val peopleStrings = resources.getStringArray(R.array.userNames)
        for (name in peopleStrings) {
            people.add(Person(name))
        }

        // Create dialog to assign items to people
        val personSelector = PersonSelectorDialogFragment()
        personSelector.setTargetFragment(this, 0)
        // Add listener to assign button
        fragmentAdapter.onAssignClick = { position ->
            currentAssignPosition = position
            personSelector.show(
                parentFragmentManager,
                "people_selector"
            )
        }


        fabSettings = view.findViewById(R.id.fab)
        layoutFabAddItem = view.findViewById(R.id.layoutFabAddItem)
        layoutFabConfirmItem = view.findViewById(R.id.layoutFabConfirm)

        //When main Fab (Settings) is clicked, it expands if not expanded already.
        //Collapses if main FAB was open already.
        //This gives FAB (Settings) open/close behavior

        //When main Fab (Settings) is clicked, it expands if not expanded already.
        //Collapses if main FAB was open already.
        //This gives FAB (Settings) open/close behavior
        fabSettings.setOnClickListener {
            if (fabExpanded) {
                closeSubMenusFab()
            } else {
                openSubMenusFab()
            }
        }
        //Only main FAB is visible in the beginning
        closeSubMenusFab()

    }

    private fun initNavigation(navigation: BottomNavigationView) {
        navigation.menu.getItem(0).isCheckable = false
        navigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {

                R.id.cameraButton -> {
                    item.isCheckable = true //here is the magic

                    //notify the listener
                    true
                }
                R.id.galleryButton -> {
                    item.isCheckable = true

                    //notify the listener
                    true
                }

                else -> false
            }
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun fixRotation(source: Bitmap, stream: InputStream): Bitmap {
        val ei = ExifInterface(stream)
        val orientation: Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(source, 90F)?.let { return it }
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(source, 180F)?.let { return it }
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(source, 270F)?.let { return it }
        }
        return source
    }

    private fun runTextRecognition(photoPath: Uri) {
        context?.let {
            activity?.let {
                // Set progress spinner to visible
                val progressSpinner = it.findViewById<ProgressBar>(R.id.spin_kit)
                val progressText = it.findViewById<TextView>(R.id.loadingText)
                progressText.visibility = View.VISIBLE
                progressSpinner.visibility = View.VISIBLE

                var bitmap: Bitmap
                val recyclerView = it.findViewById<RecyclerView>(R.id.recycler)
                recyclerView.visibility = View.VISIBLE
                // Get our full size image
                bitmap = if (Build.VERSION.SDK_INT < 28) { // For compatibility
                    MediaStore.Images.Media.getBitmap(
                        it.contentResolver,
                        photoPath
                    )
                } else {
                    val source = ImageDecoder.createSource(it.contentResolver, photoPath)
                    ImageDecoder.decodeBitmap(source)
                }
                // Ensure photo rotation is correct
                it.contentResolver.openInputStream(photoPath)?.let { stream ->
                    bitmap = fixRotation(bitmap, stream)
                }

                // Use MLKit to perform text recognition
                val image: InputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer: TextRecognizer = TextRecognition.getClient()
                recognizer.process(image)
                    .addOnSuccessListener { texts ->
                        // On text recognition parse prices out and then hide loading spinner
                        processTextRecognitionResult(texts, bitmap.height)
                        progressSpinner.visibility = View.INVISIBLE
                        progressText.visibility = View.INVISIBLE
                    }
                    .addOnFailureListener { e -> // Task failed with an exception
                        e.printStackTrace()
                        showToast("Failed to analyze receipt")
                    }
            }
        }
    }

    private fun showToast(text: String) {
        view?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun closest(of: Text.Line, blocks: List<Text.TextBlock>): Pair<Text.Line, Int> {
        var min = Int.MAX_VALUE
        var closest = of
        var diff: Int
        for (block in blocks) {
            val lines = block.lines
            for (line in lines) {
                if ((line.text.contains("$") || PRICE_REGEX.containsMatchIn(line.text))
                    && line.text != of.text
                ) {
                    diff = abs(line.boundingBox!!.bottom - of.boundingBox!!.bottom)
                    if (diff < min) {
                        min = diff
                        closest = line
                    }
                }
            }
        }
        return Pair(closest, min)
    }

    private fun processTextRecognitionResult(result: Text, height: Int) {
        val blocks: List<Text.TextBlock> = result.textBlocks
        if (blocks.isEmpty()) {
            showToast("No text found")
            return
        }

        itemsList.clear()

        for (block in blocks) {
            val lines = block.lines
            // For each detected line
            for (line in lines) {
                // If there is no $ hopefully we have an item
                if (!line.text.contains("$")) {
                    val (closestPrice, diff) = closest(line, blocks)

                    if (((diff.toDouble() / height) * 100) < 1) {
                        var price = closestPrice.text.replace('O', '0')
                        PRICE_REGEX.find(price)?.let {
                            price = '$' + it.value
                        }
                        itemsList.add(Pair(line.text, price))
                    }
                    println("${line.text} \t ${closestPrice.text}")
                    println((diff.toDouble() / height) * 100)
                    println()
                }
            }
        }
        fragmentAdapter.notifyDataSetChanged()
    }

    private fun dispatchGalleryIntent() {
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        ).also { galleryIntent ->
            galleryIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    view?.let {
                        Snackbar.make(it, "Failed to create image file.", Snackbar.LENGTH_LONG)
                            .show()
                    }
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also { pFile ->
                    activity?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            it,
                            "com.example.snapceit.fileprovider",
                            pFile
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
                ?: view?.let {
                    Snackbar.make(it, "Failed to open camera.", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CANADA).format(Date())
        activity?.cacheDir.also { storageDir ->
            return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            ).apply {
                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = toUri()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_IMAGE) {
                data?.also {
                    it.data?.also { uri ->
                        runTextRecognition(uri)
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                runTextRecognition(currentPhotoPath)
            }
        }
    }

    override fun onDialogPositiveClick(selectedPeople: ArrayList<Int>) {
        for (i in 0 until people.size) {
            if (selectedPeople.contains(i)) {
                people[i].itemPrices[i] = itemsList[i].second
            } else if (people[i].itemPrices.containsKey(i)) {
                people[i].itemPrices.remove(i)
            }
        }
        for (p in people) {
            println(p)
        }
    }


    //closes FAB submenus
    private fun closeSubMenusFab() {
        layoutFabAddItem.visibility = View.INVISIBLE
        layoutFabConfirmItem.visibility = View.INVISIBLE
        fabSettings.setImageResource(android.R.drawable.ic_input_add)
        fabExpanded = false
    }

    //Opens FAB submenus
    private fun openSubMenusFab() {
        layoutFabAddItem.visibility = View.VISIBLE
        layoutFabConfirmItem.visibility = View.VISIBLE
        //Change settings icon to 'X' icon
        fabSettings.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        fabExpanded = true
    }
}