package com.idenc.snapceit

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
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
class FirstFragment : Fragment(), PersonSelectorDialogFragment.MyDialogListener,
    AddTaxDialogFragment.MyDialogListener, AddItemDialogFragment.MyDialogListener {
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_GALLERY_IMAGE = 2
    private val PRICE_REGEX = Regex("([\\dO]+\\.\\d{1,2})")

    private lateinit var currentPhotoPath: Uri
    private lateinit var fragmentAdapter: ItemRecyclerAdapter
    private var itemsList = ArrayList<Triple<String, String, ArrayList<String>>>()
    private var people = ArrayList<Person>()
    private var currentAssignPosition: Int = -1
    private var taxPrice = 0.0

    //boolean flag to know if main FAB is in open or closed state.
    private var fabExpanded = false
    private lateinit var fabSettings: ImageButton

    //Linear layout holding the Save submenu
    private lateinit var layoutFabAddItem: LinearLayout
    private lateinit var layoutFabConfirmItem: LinearLayout
    private lateinit var layoutFabTax: LinearLayout
    private lateinit var fabMenu: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().getPreferences(Context.MODE_PRIVATE).edit().remove("select_people")
            .apply()
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
        fragmentAdapter = ItemRecyclerAdapter(itemsList)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        recycler.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = fragmentAdapter
        }

        // Instantiate our current people
        val peopleStrings = activity?.getSharedPreferences(
            "select_people",
            Context.MODE_PRIVATE
        )?.getStringSet("people_names", setOf())!!.toMutableList()
        val checkedPeoplePref = activity?.getSharedPreferences("ListFile", Context.MODE_PRIVATE)
        val numItems = checkedPeoplePref!!.getInt("checked_size", 0)
        for (i in 0 until numItems) {
            if (!checkedPeoplePref.getBoolean("checked_$i", true)) {
                peopleStrings.removeAt(i)
            }
        }
        for (name in peopleStrings) {
            people.add(Person(name))
        }

        val finalSplitDialog = FinalSplitDialogFragment(people)
        val taxDialog = AddTaxDialogFragment()
        taxDialog.setTargetFragment(this, 0)
        val addItemDialog = AddItemDialogFragment()
        addItemDialog.setTargetFragment(this, 0)

        // Create dialog to assign items to people
        val personSelector = PersonSelectorDialogFragment()
        personSelector.setTargetFragment(this, 0)
        // Add listener to assign button
        fragmentAdapter.onAssignClick = { position ->
            val imm: InputMethodManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            val current = requireActivity().currentFocus
            current?.apply {
                this.clearFocus()
            }
            currentAssignPosition = position
            personSelector.position = position
            personSelector.show(
                parentFragmentManager,
                "people_selector"
            )
        }
        fragmentAdapter.onDeleteClick = {
            this.deleteItemHandler(it)
        }
        fragmentAdapter.onEditPrice = { position, newPrice ->
            if (position < itemsList.size && position >= 0) {
                itemsList[position] = itemsList[position].copy(second = newPrice)
                for (p in people) {
                    if (p.itemPrices.containsKey(position)) {
                        p.itemPrices[position] = p.itemPrices[position]!!.copy(first = newPrice)
                    }
                }
            }
        }
        fragmentAdapter.onEditName = { position, newName ->
            if (position < itemsList.size && position >= 0) {
                itemsList[position] = itemsList[position].copy(first = newName)
            }
        }

        fabSettings = view.findViewById(R.id.actionButton)
        layoutFabAddItem = view.findViewById(R.id.layoutFabAddItem)
        layoutFabConfirmItem = view.findViewById(R.id.layoutFabConfirm)
        layoutFabTax = view.findViewById(R.id.layoutFabTax)
        fabMenu = view.findViewById(R.id.fabMenu)

        recycler.setOnTouchListener { recycleView: View, motionEvent: MotionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val outRect = Rect()
                fabMenu.getGlobalVisibleRect(outRect)
                if (fabExpanded && !outRect.contains(
                        motionEvent.rawX.toInt(),
                        motionEvent.rawY.toInt()
                    )
                ) {
                    closeSubMenusFab()
                }
            }
            recycleView.performClick()
        }

        layoutFabConfirmItem.setOnClickListener {
            var totalPrice = 0.0
            people.forEach { person ->
                person.accumulatePrice()
                totalPrice += person.owedPrice
            }
            // Find the tax percentage
            var taxPercent = 0.0
            if (totalPrice > 0) {
                taxPercent = taxPrice / totalPrice
            }
            if (taxPercent > 0) {
                // Add tax percentage to each person's items
                // to split tax based on what people paid for
                for (person in people) {
                    person.owedPrice += person.owedPrice * taxPercent
                }
            }
            finalSplitDialog.show(parentFragmentManager, "person_split")
        }

        layoutFabTax.setOnClickListener {
            taxDialog.show(parentFragmentManager, "add_tax")
        }

        layoutFabAddItem.setOnClickListener {
            addItemDialog.show(parentFragmentManager, "add_item")
        }

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
                        fabMenu.visibility = View.VISIBLE
                        fabSettings.visibility = View.VISIBLE
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
                        var price = closestPrice.text
                            .replace('O', '0')
                            .replace(',', '.')
                        PRICE_REGEX.find(price)?.let {
                            price = '$' + it.value
                        }
                        itemsList.add(Triple(line.text, price, ArrayList()))
                    }
                    // println("${line.text} \t ${closestPrice.text}")
                    // println((diff.toDouble() / height) * 100)
                    // println()
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
            requireActivity().packageManager?.also {
                startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            requireActivity().packageManager?.also {
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

    private fun deleteItemHandler(position: Int) {
        for (p in people) {
            if (p.itemPrices.containsKey(position)) {
                p.itemPrices.remove(position)
            } else {
                val newHashMap = HashMap<Int, Pair<String, Int>>()
                for ((key, value) in p.itemPrices) {
                    if (key >= position) {
                        newHashMap[key - 1] = value
                    } else {
                        newHashMap[key] = value
                    }
                }
                p.itemPrices = newHashMap
            }
        }
    }

    override fun onPersonDialogPositiveClick(selectedPeople: ArrayList<Int>) {
        for (i in 0 until people.size) {
            if (selectedPeople.contains(i)) {
                people[i].itemPrices[currentAssignPosition] = Pair(
                    itemsList[currentAssignPosition].second,
                    selectedPeople.size
                )
                if (!itemsList[currentAssignPosition].third.contains(people[i].name)) {
                    itemsList[currentAssignPosition].third.add(people[i].name)
                }
            } else if (people[i].itemPrices.containsKey(currentAssignPosition)) {
                people[i].itemPrices.remove(currentAssignPosition)
                itemsList[currentAssignPosition].third.remove(people[i].name)
            }
        }
        fragmentAdapter.notifyItemChanged(currentAssignPosition)
    }

    override fun onTaxDialogPositiveClick(enteredTax: Double) {
        taxPrice = enteredTax
    }

    override fun onAddItemDialogPositiveClick(itemName: String, itemPrice: String) {
        itemsList.add(Triple(itemName, itemPrice, ArrayList()))
        fragmentAdapter.notifyItemInserted(itemsList.size - 1)
    }

    //closes FAB submenus
    private fun closeSubMenusFab() {
        layoutFabAddItem.visibility = View.INVISIBLE
        layoutFabConfirmItem.visibility = View.INVISIBLE
        layoutFabTax.visibility = View.INVISIBLE
        fabExpanded = false
    }

    //Opens FAB submenus
    private fun openSubMenusFab() {
        layoutFabAddItem.visibility = View.VISIBLE
        layoutFabConfirmItem.visibility = View.VISIBLE
        layoutFabTax.visibility = View.VISIBLE
        //Change settings icon to 'X' icon
        fabExpanded = true
    }
}