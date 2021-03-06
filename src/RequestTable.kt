package burp

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.Timer
import javax.swing.border.BevelBorder
import javax.swing.table.TableRowSorter


class UpdateStatusbar(val message: JLabel, val handler: AttackHandler): ActionListener {
    lateinit var timer: Timer

    override fun actionPerformed(e: ActionEvent?) {
        if (handler.hasFinished() || SwingUtilities.getWindowAncestor(message) == null){
            timer.stop()
        }

        message.text = handler.statusString()
    }

}

interface OutputHandler {
    fun add(req: Request)
}

class ConsolePrinter: OutputHandler {
    private val requestID = AtomicInteger(0)

    init {
        Utils.out("ID | Word | Status | Wordcount | Length")
    }

    override fun add(req: Request) {
        Utils.out(String.format("%s | %s | %s | %s | %s ", requestID.incrementAndGet(), req.word?: "", req.code, req.wordcount, req.length))
    }
}

class RequestTable(val service: IHttpService, val handler: AttackHandler): JPanel(), OutputHandler {
    val model = RequestTableModel()
    val issueTable = JTable(model)
    val requestEditor: IMessageEditor
    val responseEditor: IMessageEditor
    val bottomSplit: JSplitPane
    val requestListView: JScrollPane
    private val controller = MessageEditorController()
    private var currentRequest: Request? = null

    fun setCurrentRequest(req: Request?) {
        //println("Setting current request to "+req!!.word)
        currentRequest = req!!
        requestEditor.setMessage(req.getRequestAsBytes(), true)
        responseEditor.setMessage(req.getResponseAsBytes(), false)
    }

    init {

        issueTable.rowSorter = TableRowSorter(model)

        issueTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        //issueTable.getColumnModel().getColumn(0).setPreferredWidth(500)

        issueTable.getSelectionModel().addListSelectionListener({
            val req = model.getRequest(issueTable.getSelectedRow())
            setCurrentRequest(req!!)
        })

        requestListView = JScrollPane(issueTable)

        requestEditor = Utils.callbacks.createMessageEditor(controller, false)
        responseEditor = Utils.callbacks.createMessageEditor(controller, false)
        bottomSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestEditor.getComponent(), responseEditor.getComponent())
        bottomSplit.setResizeWeight(0.5)


        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, requestListView, bottomSplit)

        requestListView.preferredSize = Dimension(1280, 400)
        splitPane.setDividerLocation(0.2)
        splitPane.preferredSize = Dimension(1280, 800)

        this.layout = BorderLayout()
        this.add(splitPane, BorderLayout.CENTER)


        splitPane.resizeWeight = 0.5


        val statusPanel = JPanel()
        statusPanel.border = BevelBorder(BevelBorder.LOWERED)
        this.add(statusPanel, BorderLayout.SOUTH) //
        statusPanel.preferredSize = Dimension(this.getWidth(), 30)
        statusPanel.layout = BoxLayout(statusPanel, BoxLayout.X_AXIS)
        val statusLabel = JLabel("")
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT)
        statusPanel.add(statusLabel)

        val updateStatusbar = UpdateStatusbar(statusLabel, handler)
        val panelUpdater = Timer(1000, updateStatusbar)
        updateStatusbar.timer = panelUpdater
        panelUpdater.start()

        Utils.callbacks.customizeUiComponent(this)
        Utils.callbacks.customizeUiComponent(issueTable)
    }


    override fun add(req: Request) {
        model.addRequest(req)
    }

    inner class MessageEditorController : IMessageEditorController {
        override fun getHttpService(): IHttpService? {
            return service //currentRequest.getHttpService()
        }

        override fun getRequest(): ByteArray? {
            return currentRequest?.getRequestAsBytes()
        }

        override fun getResponse(): ByteArray? {
            return currentRequest?.getResponseAsBytes()
        }
    }

}


