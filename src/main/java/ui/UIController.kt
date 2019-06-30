package ui

import bezier.GraphicalBezier
import bezier.Point
import bezier.units.Inches
import bezier.units.Pixels
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.chart.LineChart
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Tab
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.shape.Circle
import javafx.scene.shape.Polyline
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import utils.CSVWriter
import utils.Config
import utils.Utils.aboutEquals
import java.awt.GraphicsEnvironment
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.MalformedURLException
import java.util.*
import java.util.stream.Collectors.toCollection

class UIController {
    @FXML
    private val paneImg: AnchorPane? = null

    @FXML
    private val tabVel: Tab? = null

    @FXML
    private val root: Pane? = null

    @FXML
    private val cursorHighlight: Circle? = null
    @FXML
    private val pointHighlight: Circle? = null

    @FXML
    private val polyPos: Polyline? = null
    @FXML
    private val polyLeft: Polyline? = null
    @FXML
    private val polyRight: Polyline? = null

    @FXML
    private val chtLeft: LineChart<Double, Double>? = null
    @FXML
    private val chtRight: LineChart<Double, Double>? = null
    @FXML
    private val chtCenter: LineChart<Double, Double>? = null

    @FXML
    private val imgField: ImageView? = null

    @FXML
    private val grdPoints: GridPane? = null

    //config values
    @FXML
    private val cfgRadius: TextField? = null
    @FXML
    private val cfgWidth: TextField? = null
    @FXML
    private val cfgTicksPerInch: TextField? = null
    @FXML
    private val cfgLength: TextField? = null
    @FXML
    private val cfgMaxVel: TextField? = null
    @FXML
    private val cfgMaxAccel: TextField? = null
    @FXML
    private val cfgJerk: TextField? = null
    @FXML
    private val cfgTimeStep: TextField? = null
    @FXML
    private val cfgPathName: TextField? = null
    @FXML
    private val cfgDrawWheelType: ComboBox<String>? = null
    private var previousStates: ArrayList<ArrayList<PointRow>>? = null //for undo/redo
    private var rows: ArrayList<PointRow>? = null
    private var nextIndex: Int = 0
    private var gridDnDIndex = -1
    //-1 means nothing is being dragged
    private var dragStartIndex: Int = 0
    private var currentState: Int = 0
    private var draggedRow: PointRow? = null
    private var graph: GraphingUtil? = null
    private var pointDrag = false
    private var isClicking: Boolean = false

    @FXML
    private fun initialize() {
        cfgDrawWheelType!!.setItems(FXCollections.observableArrayList(
                *Arrays.stream(WheelPathType.values())
                        .map<String>(Function<WheelPathType, String> { it.toString() })
                        .toArray(String[]::new  /* Currently unsupported in Kotlin */)
        ))
        config = Config(cfgDrawWheelType, cfgLength, cfgMaxAccel, cfgMaxVel, cfgJerk,
                cfgRadius, cfgWidth, cfgTimeStep, cfgPathName, cfgTicksPerInch)

        imgField!!.image = backgroundImage

        rows = ArrayList()
        previousStates = ArrayList()
        setNextIndex(-1)
        grdPoints!!.setOnDragOver { event ->
            if (gridDnDIndex == -1) return@grdPoints.setOnDragOver
            val y = event.y
            val rowHeight = grdPoints.vgap + rows!![0].comboBox.height
            gridDnDIndex = Math.floor(y / rowHeight).toInt()
            dndHandling(draggedRow, false)
            graph!!.updateAndGraph(pointDrag)
        }
        tabVel!!.setOnSelectionChanged { event -> graph!!.graphMotion() }
        graph = GraphingUtil(ArrayList(), ArrayList(), rows,
                polyPos, polyLeft, polyRight,
                chtLeft, chtRight, chtCenter,
                tabVel, paneImg)

        val openPointsTask = object : Task<Void>() {
            override fun call(): Void? {
                var ran = false
                while (!ran) {
                    if (isCancelled) break
                    if (root == null) continue
                    if (root.children.stream().anyMatch(Predicate<Node> { Objects.isNull(it) })) continue
                    ran = true

                    // Load in most recently opened path
                    val file = File(Config.getStringProperty("points_save_dir") + "/" + Config.getStringProperty("path_name") + "_save.csv")
                    Objects.requireNonNull<ArrayList<Point>>(pointsFromFile(file)).forEach { p -> addNewPointRow(p, false) }
                    graph!!.updateAndGraph(pointDrag)
                    addSavedState()

                    //size background image
                    var width = backgroundImage.width
                    var height = backgroundImage.height
                    //java.awt.Toolkit.getDefaultToolkit.getScreenSize() was giving the wrong values for me
                    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
                    val percentSize = 0.7 // Estimated percent of screen to fill
                    val maxHeight = screen.height * percentSize
                    val maxWidth = screen.width * percentSize

                    val proportion = Math.min(maxHeight / height, maxWidth / width)

                    width *= proportion
                    height *= proportion

                    imgField.fitWidth = width
                    imgField.fitHeight = height
                }

                return null
            }
        }

        Thread(openPointsTask).start()
    }

    @FXML
    private fun btnNewPointEvent() {
        addNewPointRow(Point(0.0, 0.0), true)
    }

    private fun addNewPointRow(point: Point, save: Boolean) {
        val row = PointRow(rows!!.size, point)
        row.allNodes.forEach { node -> pointRowListeners(node, row) }
        rows!!.add(row)
        if (save) addSavedState()
        grdPoints!!.children.addAll(row.allNodes)
    }

    private fun addSavedState() {
        if (currentState != previousStates!!.size - 1)
            previousStates!!.removeIf { pointRows -> previousStates!!.indexOf(pointRows) > currentState }

        previousStates!!.add(ArrayList())
        rows!!.forEach { row -> previousStates!![previousStates!!.size - 1].add(PointRow(row.index, row.point)) }
        previousStates!![previousStates!!.size - 1]
                .forEach { row ->
                    row.allNodes
                            .forEach { node -> pointRowListeners(node, row) }
                }
        currentState = previousStates!!.size - 1
    }

    private fun pointRowListeners(node: Node, row: PointRow) {
        if (node is TextField)
            node.setOnKeyReleased { event ->
                row.updatePoint()
                addSavedState()
                graph!!.updateAndGraph(pointDrag)
            }
        if (node is CheckBox)
            node.setOnAction { event ->
                row.updatePoint()
                addSavedState()
                graph!!.updateAndGraph(pointDrag)
            }
        if (node is ComboBox<*>)
            node.setOnAction { event ->
                handleComboResults(row.comboBox.value, row.index)
                node.selectionModel.clearSelection()
                root!!.requestFocus() //to prevent double selection
            }

        //Drag and Drop listeners
        node.setOnDragDetected { event ->
            gridDnDIndex = row.index
            draggedRow = row
            dragStartIndex = row.index
            val db = node.startDragAndDrop(TransferMode.MOVE)
            val content = ClipboardContent()
            content.putString("this has to exist or nothing works! :( ") //<---- self documenting code
            db.setContent(content)
        }
        node.setOnDragDone { event ->
            dndHandling(row, draggedRow!!.index != dragStartIndex)
            graph!!.updateAndGraph(pointDrag)
        }
    }

    private fun handleComboResults(res: String, index: Int) {
        if (nextIndex != -1)
            return
        val result = Arrays.stream(PointMenuResult.values())
                .filter { pmr -> pmr.toString() == res }
                .findFirst()
                .orElse(PointMenuResult.NONE)

        when (result) {
            PointMenuResult.MENU -> {
                val menu = PopupFactory.menu(rowAtIndex(index).point)
                menu.showAndWait().ifPresent(Consumer<Point> { rowAtIndex(index).point = it })
                addSavedState()
                graph!!.updateAndGraph(pointDrag)
            }
            PointMenuResult.DELETE_POINT -> deletePoints(index, index)
            PointMenuResult.POINT_MOVE_MODE -> {
                setNextIndex(index)
                pointHighlight!!.centerX = rowAtIndex(index).point.x.pixels().value
                pointHighlight.centerY = imageHeight().minus(rowAtIndex(index).point.y.pixels()).getValue()
            }
            PointMenuResult.TOGGLE_OVERRIDE_VEL -> {
                rowAtIndex(index).point.isOverrideMaxVel = !rowAtIndex(index).point.isOverrideMaxVel
                addSavedState()
                graph!!.updateAndGraph(pointDrag)
            }
            PointMenuResult.TOGGLE_BACKWARDS -> {
                rowAtIndex(index).point.isReverse = !rowAtIndex(index).point.isReverse
                addSavedState()
                graph!!.updateAndGraph(pointDrag)
            }
            PointMenuResult.MAX_VEL -> {
                if (!rowAtIndex(index).point.isIntercept) return
                val maxVel = GraphicalBezier.calcMaxVel(graph!!.controlPoints, index)
                rowAtIndex(index).point.targetVelocity = maxVel
                rowAtIndex(index).updateDisplay()
                graph!!.updateAndGraph(pointDrag)
                addSavedState()
            }
        }
    }

    private fun dndHandling(draggedRow: PointRow?, save: Boolean) {
        for (r in rows!!) {
            if (gridDnDIndex < draggedRow!!.index) {
                if (r.index >= gridDnDIndex && r.index < draggedRow.index) {
                    r.moveIndex(-1)
                }
            } else if (gridDnDIndex > draggedRow.index) {
                if (r.index <= gridDnDIndex && r.index > draggedRow.index) {
                    r.moveIndex(1)
                }
            }
        }
        draggedRow!!.index = gridDnDIndex
        if (save) addSavedState()
    }

    @FXML
    private fun deleteLastPoint() {
        deletePoints(rows!!.size - 1, rows!!.size - 1)
    }

    @FXML
    private fun deleteAllPoints() {
        deletePoints(0, rows!!.size - 1)
    }

    @FXML
    private fun mnuDeleteAll() {
        cfgPathName!!.text = ""
        config!!.updateConfig()
        deleteAllPoints()
    }

    /**
     * deletes a number of PointRows equal to endIndex - startIndex + 1.
     *
     * to delete one point, startIndex and endIndex should be equal
     */
    private fun deletePoints(startIndex: Int, endIndex: Int) {
        if (nextIndex >= startIndex && nextIndex <= endIndex) setNextIndex(-1)
        for (i in endIndex downTo startIndex) {
            val currentRow = rowAtIndex(i)
            grdPoints!!.children.removeAll(currentRow.allNodes)
            rows!!.remove(currentRow)
        }
        rows!!.stream()
                .filter { row -> row.index > endIndex }
                .forEach { row -> row.moveIndex(endIndex - startIndex + 1) }
        graph!!.updateAndGraph(pointDrag)
        addSavedState()
    }

    private fun rowAtIndex(index: Int): PointRow {
        return rows!!.parallelStream()
                .filter { row -> row.index == index }
                .findFirst()
                .orElseThrow()
    }

    @FXML
    private fun releaseEvent(mouseEvent: MouseEvent) {
        isClicking = false
        if (pointDrag) {
            dragPoint(mouseEvent)
        } else {
            addPoint(mouseEvent)
        }
    }

    private fun dragPoint(mouseEvent: MouseEvent) {
        val x = Pixels(mouseEvent.x)
        var y = Pixels(mouseEvent.y)
        if (x.getValue() < 0 || y.getValue() < 0 || x.getValue() > imageWidth().getValue() || y.getValue() > imageHeight().getValue())
            return
        y = imageHeight().minus(y)
        //
        //        System.out.println("x = " + x);
        //        System.out.println("y = " + y);
    }

    private fun addPoint(mouseEvent: MouseEvent) {
        val x = Pixels(mouseEvent.x)
        var y = Pixels(mouseEvent.y)
        val intercept = mouseEvent.button == MouseButton.PRIMARY && !mouseEvent.isControlDown
        if (x.getValue() < 0 || y.getValue() < 0 || x.getValue() > imageWidth().getValue() || y.getValue() > imageHeight().getValue())
            return
        y = imageHeight().minus(y.getValue())
        if (nextIndex == -1) {
            addNewPointRow(Point(x.inches(), y.inches(), intercept), true)
        } else {
            rowAtIndex(nextIndex).point = Point(x.inches(), y.inches(), rowAtIndex(nextIndex).point.isIntercept)
            addSavedState()
            setNextIndex(-1)
        }
        graph!!.updateAndGraph(pointDrag)
    }

    @FXML
    private fun mouseMoveEvent(event: MouseEvent) {
        //highlight only appears if circles are visible
        cursorHighlight!!.centerX = Math.max(0, Math.min(imageWidth().getValue(), event.x))
        cursorHighlight.centerY = Math.max(0, Math.min(imageHeight().getValue(), event.y))

        //drag point if in pointDragMode
        if (pointDrag) { //todo detect if it is clicked
            graph!!.rows.stream()
                    .filter { pr -> aboutEquals(pr.point.x.pixels().value, event.x, 9.0) }
                    .filter { pr -> aboutEquals(imageHeight().minus(pr.point.y.pixels()).getValue(), event.y, 9.0) }
                    .findFirst()
                    .ifPresent { pr ->
                        pr.point.x = Pixels(event.x).inches()
                        pr.point.y = imageHeight().minus(Pixels(event.y)).inches()

                        pr.updateDisplay()
                    }
            graph!!.updateAndGraph(true)
        }
    }

    @FXML
    private fun keyReleasedEvent(keyEvent: KeyEvent) {
        if (nextIndex != -1) {
            val row = rowAtIndex(nextIndex)
            root!!.requestFocus()
            val ctrl = keyEvent.isControlDown
            val shift = keyEvent.isShiftDown
            val change = if (shift) if (ctrl) 20 else 1 else if (ctrl) 10 else 5 //key = in -> shift=1, none=6, ctrl=12, both=24
            var x = row.point.x
            var y = row.point.y
            when (keyEvent.code) {
                KeyCode.UP -> y = Inches(y.getValue() + change)
                KeyCode.DOWN -> y = Inches(y.getValue() - change)
                KeyCode.LEFT -> x = Inches(x.getValue() - change)
                KeyCode.RIGHT -> x = Inches(x.getValue() + change)
                KeyCode.ENTER -> {
                    setNextIndex(-1)
                    addSavedState()
                }
                KeyCode.ESCAPE -> setNextIndex(-1)
            }
            row.point = Point(x, y, row.point.isIntercept)
            pointHighlight!!.centerX = x.pixels().getValue()
            pointHighlight.centerY = imageHeight().minus(y.pixels()).getValue()
            graph!!.updateAndGraph(pointDrag)
        } else {
            var pointsFocused = false
            var focusedIndex = 0
            var focusedColumn = 0
            for (row in rows!!) {
                for (i in 0 until row.allNodes.size) {
                    if (row.allNodes[i].isFocused) {
                        pointsFocused = true
                        focusedIndex = row.index
                        focusedColumn = i
                    }
                }
            }
            if (!pointsFocused) return
            when (keyEvent.code) {
                KeyCode.UP -> rowAtIndex(Math.max(0, focusedIndex - 1))
                        .allNodes[focusedColumn]
                        .requestFocus()
                KeyCode.DOWN -> rowAtIndex(Math.min(rows!!.size - 1, focusedIndex + 1))
                        .allNodes[focusedColumn]
                        .requestFocus()
            }
        }
    }

    private fun setNextIndex(nextIndex: Int) {
        pointHighlight!!.isVisible = nextIndex != -1
        cursorHighlight!!.isVisible = nextIndex != -1
        this.nextIndex = nextIndex
    }

    @FXML
    private fun mnuOpenImage() {
        val fileChooser = FileChooser()
        fileChooser.title = "Open Field Image"
        //        String imgDir = Config.getStringProperty("img_path", "src\\images");
        //        if (imgDir.contains("."))
        //            imgDir = imgDir.substring(0, imgDir.indexOf("."));
        //        fileChooser.setInitialDirectory(new File(imgDir));
        fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.jpeg", "*.gif", "*.bmp", "*.pdn"),
                FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg"),
                FileChooser.ExtensionFilter("PNG", "*.png")
        )
        val chosenImage = fileChooser.showOpenDialog(root!!.scene.window)
        //        config.setProperty("img_path", chosenImage.getAbsolutePath());
        try {
            backgroundImage = Image(chosenImage.toURI().toURL().toString())
        } catch (e: MalformedURLException) {
            //fail silently, just means they clicked cancel or X on the popup
        }

        imgField!!.image = backgroundImage
        imgField.fitWidth = imageWidth().getValue()
        imgField.fitHeight = imageHeight().getValue()
    }

    @FXML
    private fun mnuExport() {
        export(graph!!.path, Config.getStringProperty("path_name"))
    }

    private fun export(pathPoints: ArrayList<Point>, pathName: String) {
        val url = Config.getStringProperty("csv_out_dir") + "\\" + pathName
        try {
            CSVWriter<Point>(url + "_left.csv").use { leftWriter ->
                CSVWriter<Point>(url + "_right.csv").use { rightWriter ->
                    val firstHeading = pathPoints[0].heading.value
                    leftWriter.writeObjects("Dist,Vel,Heading", pathPoints,
                            { p -> -p.leftPos.ticks().value },
                            { p -> -p.leftVel.ticksPerHundredMillis().getValue() },
                            { p -> p.heading.value - firstHeading })
                    rightWriter.writeObjects("Dist,Vel,Heading", pathPoints,
                            { p -> p.rightPos.ticks().value },
                            { p -> p.rightVel.ticksPerHundredMillis().getValue() },
                            { p -> p.heading.value - firstHeading })
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @FXML
    private fun mnuSavePoints() {
        savePoints(graph!!.controlPoints)
    }

    private fun savePoints(controlPoints: ArrayList<Point>) {
        val url = Config.getStringProperty("points_save_dir") + "\\" + Config.getStringProperty("path_name")
        try {
            CSVWriter<Point>(url + "_save.csv").use { writer ->
                writer.writeObjects("X,Y,Intercept,Velocity,Override,Reverse", controlPoints,
                        { p -> p.x.value },
                        { p -> p.y.value },
                        Function<Point, Any> { it.isIntercept() },
                        { p -> p.targetVelocity.getValue() },
                        Function<Point, Any> { it.isOverrideMaxVel() },
                        Function<Point, Any> { it.isReverse() })
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * recalculates all paths in current save directory, in case config values have changed or something
     */
    @FXML
    private fun updateAllPaths() {
        val files = File(Config.getStringProperty("points_save_dir")).listFiles { pathname -> pathname.absolutePath.endsWith("_save.csv") }
        for (file in Objects.requireNonNull(files)) {
            val points = pointsFromFile(file)
            if (points != null) {
                export(GraphicalBezier.generateSpline(points), pathNameFromFile(file))
            }
        }
    }

    @FXML
    private fun mnuOpenPoints() {
        val saveFile = FileChooser()
        saveFile.title = "Choose save"

        saveFile.initialDirectory = File(Config.getStringProperty("points_save_dir"))
        val file = saveFile.showOpenDialog(root!!.scene.window)
        if (file != null) {
            deleteAllPoints()
            val newPoints = pointsFromFile(file)
            if (newPoints != null) {
                graph!!.rows.clear()
                newPoints.forEach { pt -> addNewPointRow(pt, false) }
                addSavedState()
                graph!!.updateAndGraph(pointDrag)
                cfgPathName!!.text = pathNameFromFile(file)
                config!!.updateConfig()
            }
        }
    }


    private fun pointsFromFile(file: File): ArrayList<Point>? {
        try {
            BufferedReader(FileReader(file)).use { reader ->
                return reader.lines()
                        .skip(1)
                        .map { line -> line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() }
                        .map { vals ->
                            Point(
                                    java.lang.Double.parseDouble(vals[0]),
                                    java.lang.Double.parseDouble(vals[1]),
                                    java.lang.Boolean.parseBoolean(vals[2]),
                                    LinearVelocity(Inches(java.lang.Double.parseDouble(vals[3])), Seconds(1.0)),
                                    java.lang.Boolean.parseBoolean(vals[4]),
                                    java.lang.Boolean.parseBoolean(vals[5])
                            )
                        }
                        .collect<ArrayList<Point>, Any>(toCollection(Supplier<ArrayList<Point>> { ArrayList() }))
            }
        } catch (e: IOException) {
            //fail silently, just means the selected file doesn't exist
        }

        return null
    }

    private fun pathNameFromFile(file: File): String {
        return file.name.substring(0, file.name.length - "_save.csv".length)
    }

    @FXML
    private fun undo() {
        if (currentState == 0)
            return
        grdPoints!!.children.clear()
        rows!!.clear()
        currentState--
        for (row in previousStates!![currentState]) {
            row.updatePoint()
            rows!!.add(row)
            grdPoints.children.addAll(row.allNodes)
            row.allNodes.forEach { node -> GridPane.setRowIndex(node, row.index) }
        }
        graph!!.updateAndGraph(pointDrag)
    }

    @FXML
    private fun redo() {
        if (currentState == previousStates!!.size - 1)
            return
        grdPoints!!.children.clear()
        rows!!.clear()
        currentState++
        for (row in previousStates!![currentState]) {
            row.updatePoint()
            rows!!.add(row)
            grdPoints.children.addAll(row.allNodes)
            row.allNodes.forEach { node -> GridPane.setRowIndex(node, row.index) }
        }
        graph!!.updateAndGraph(pointDrag)
    }

    @FXML
    private fun configUpdate() {
        config!!.updateConfig()
    }

    @FXML
    private fun mnuChangeCSVOut() {
        configUpdate()
        val dirChooser = DirectoryChooser()
        dirChooser.title = "CSV Generation Location"
        dirChooser.initialDirectory = File(Config.getStringProperty("csv_out_dir", "src"))
        val dir = dirChooser.showDialog(root!!.scene.window)
        config!!.setProperty("csv_out_dir", dir.absolutePath)
    }

    @FXML
    private fun mnuChangeSaveOut() {
        configUpdate()
        val dirChooser = DirectoryChooser()
        dirChooser.title = "Save location"
        dirChooser.initialDirectory = File(Config.getStringProperty("points_save_dir", "src"))
        val dir = dirChooser.showDialog(root!!.scene.window)
        config!!.setProperty("points_save_dir", dir.absolutePath)
    }

    @FXML
    private fun redraw() {
        graph!!.updateAndGraph(pointDrag)
    }

    @FXML
    private fun mnuPointDrag() {
        pointDrag = !pointDrag
        graph!!.updateAndGraph(pointDrag)
        if (!pointDrag) {
            graph!!.clearCircles()
            addSavedState()
        }
    }

    @FXML
    private fun mnuSaveAll() {
        if (Config.getStringProperty("path_name").isEmpty()) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Path must be named"
            return
        }
        mnuSavePoints()
        mnuExport()
    }

    companion object {
        private var backgroundImage = Image("images/Field.JPG")
        private var config: Config? = null

        fun imageHeight(): Pixels = Pixels(backgroundImage.height)

        fun imageWidth(): Pixels = Pixels(backgroundImage.width)
    }
}
