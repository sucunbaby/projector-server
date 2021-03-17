/*
 * GNU General Public License version 2
 *
 * Copyright (C) 2019-2021 JetBrains s.r.o.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.projector.server.ProjectorServer
import org.jetbrains.projector.server.core.util.getHostName
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.net.Inet6Address
import java.net.NetworkInterface
import javax.swing.*
import kotlin.random.Random

class SessionDialog(project: Project?) : DialogWrapper(project) {
  private val description = JLabel()
  private val myHostsList: HostsList = HostsList("Host:", ProjectorService.host)

  private val urlHostsList: HostsList = HostsList("URL address:", null)
  private val portEditor: PortEditor = PortEditor(ProjectorService.port)
  private val rwTokenEditor: TokenEditor = TokenEditor("Require password for read-write access:")
  private val roTokenEditor: TokenEditor = TokenEditor("Require password for read-only access: ")
  private val rwInvitationLink: InvitationLink = InvitationLink()
  private val roInvitationLink: InvitationLink = InvitationLink()
  private val roInvitationTitle = JLabel("Read Only Link:")

  private val bothAccess = JRadioButton("RW & RO")
  private val onlyRwAccess = JRadioButton("RW only")
  private val accessGroup = ButtonGroup().apply {
    fun changeRoVisibility(isVisible: Boolean) {
      roTokenEditor.isVisible = isVisible
      roInvitationLink.isVisible = isVisible
      roInvitationTitle.isVisible = isVisible
    }
    bothAccess.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        roTokenEditor.token = generatePassword()
        changeRoVisibility(true)
        updateInvitationLinks()
      }
    }
    onlyRwAccess.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        roTokenEditor.token = rwTokenEditor.token
        changeRoVisibility(false)
        updateInvitationLinks()
      }
    }
    bothAccess.isSelected = true
    add(bothAccess)
    add(onlyRwAccess)
  }

  val rwToken: String? get() = rwTokenEditor.token
  val roToken: String? get() = roTokenEditor.token
  val listenAddress: String get() = myHostsList.selected?.address ?: ""
  val listenPort: String get() = portEditor.value
  private val urlAddress: String get() = urlHostsList.selected?.address ?: ""

  init {
    if (ProjectorService.isSessionRunning) {
      title = "Edit Current Session Parameters"
      description.text = "<html>The current session has already started.<br>Do you want to change passwords?"
      myOKAction.putValue(Action.NAME, "Save")

      portEditor.isEnabled = false
      myHostsList.isEnabled = false
      rwTokenEditor.token = ProjectorService.currentSession.rwToken
      roTokenEditor.token = ProjectorService.currentSession.roToken
    }
    else {
      title = "Start Remote Access to IDE"
      description.text = "<html>Config remote access to IDE.<br>Listen on:"
      myOKAction.putValue(Action.NAME, "Start")

      rwTokenEditor.token = generatePassword()
      roTokenEditor.token = generatePassword()
    }

    myHostsList.onChange = ::updateURLList
    urlHostsList.onChange = ::updateInvitationLinks
    portEditor.onChange = ::updateInvitationLinks
    rwTokenEditor.onChange = ::updateInvitationLinks
    roTokenEditor.onChange = ::updateInvitationLinks

    updateURLList()
    updateInvitationLinks()
    setResizable(false)
    init()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel()
    LinearPanelBuilder(panel).addNextComponent(description, gridWidth = 4, bottomGap = 5)
      .startNextLine().addNextComponent(myHostsList, gridWidth = 2, weightx = 0.5, rightGap = 15)
      .addNextComponent(portEditor, gridWidth = 2, weightx = 0.5)
      .startNextLine().addNextComponent(JLabel("Access Types:"), topGap = 5).addNextComponent(bothAccess, topGap = 5)
      .addNextComponent(onlyRwAccess, topGap = 5)
      .startNextLine().addNextComponent(rwTokenEditor.requiredCheckBox, gridWidth = 2)
      .addNextComponent(rwTokenEditor.tokenTextField, gridWidth = 2)
      .startNextLine().addNextComponent(roTokenEditor.requiredCheckBox, gridWidth = 2)
      .addNextComponent(roTokenEditor.tokenTextField, gridWidth = 2)
      .startNextLine().addNextComponent(urlHostsList, gridWidth = 2, weightx = 0.5, rightGap = 15)
      .startNextLine().addNextComponent(JLabel("Invitation Links:"), gridWidth = 4, topGap = 5, bottomGap = 5)
      .startNextLine().addNextComponent(JLabel("Full Access Link:")).addNextComponent(rwInvitationLink.link, gridWidth = 2)
      .addNextComponent(rwInvitationLink.copyButton)
      .startNextLine().addNextComponent(roInvitationTitle).addNextComponent(roInvitationLink.link, gridWidth = 2)
      .addNextComponent(roInvitationLink.copyButton)
      .startNextLine().addNextComponent(ConnectionPanel(), gridWidth = 4)
    return panel
  }

  private fun updateInvitationLinks() {
    if (onlyRwAccess.isSelected) {
      roTokenEditor.token = rwTokenEditor.token
    }
    else if (rwTokenEditor.token == roTokenEditor.token) {
      onlyRwAccess.isSelected = true
    }

    rwInvitationLink.update(urlAddress, listenPort, rwTokenEditor.token)
    roInvitationLink.update(urlAddress, listenPort, roTokenEditor.token)
  }

  private fun updateURLList() {
    val host = myHostsList.selected

    when {
      host == ALL_HOSTS -> {
        urlHostsList.setTooltip(null)
        urlHostsList.isEnabled = true
        val oldValue = urlHostsList.selected
        urlHostsList.setItems(getHostList())
        urlHostsList.setSelectedItem(oldValue)

      }
      host != null -> {
        urlHostsList.setItems(listOf(host))
        urlHostsList.setSelectedItem(host)
        urlHostsList.setTooltip("nothing to select from")
        urlHostsList.isEnabled = false
      }
      else -> {
        urlHostsList.setTooltip(null)
        urlHostsList.isEnabled = true
        urlHostsList.clear()
      }
    }

    urlHostsList.onChange?.invoke()
  }

  private class InvitationLink {
    val copyButton = JButton(AllIcons.Actions.Copy).apply {
      addActionListener {
        Toolkit
          .getDefaultToolkit()
          .systemClipboard
          .setContents(StringSelection(link.text), null)
      }
    }

    val link: JTextField = JTextField(null).apply {
      isEditable = false
      background = null
      border = null
      columns = 35
    }

    fun update(host: String, port: String, token: String?) {
      link.text = "http://${host}:${port}" + if (token == null) "" else "/?token=${token}"
    }

    var isVisible = true
      set(value) {
        link.isVisible = value
        copyButton.isVisible = value
        field = value
      }
  }

  private class TokenEditor(title: String) {
    val requiredCheckBox: JCheckBox = JCheckBox(title).apply {
      addActionListener {
        tokenTextField.text = if (isSelected) generatePassword() else null
        tokenTextField.isEnabled = isSelected
        onChange?.invoke()
      }
    }
    val tokenTextField: JTextField = JTextField().apply {
      addKeyListener(object : KeyAdapter() {
        override fun keyReleased(e: KeyEvent) {
          onChange?.invoke()
        }
      })
    }

    var onChange: (() -> Unit)? = null

    var token
      get() = if (requiredCheckBox.isSelected) tokenTextField.text else null
      set(value) {
        requiredCheckBox.isSelected = value != null
        tokenTextField.isEnabled = requiredCheckBox.isSelected
        tokenTextField.text = value
      }

    var isVisible = true
      set(value) {
        tokenTextField.isVisible = value
        requiredCheckBox.isVisible = value
        field = value
      }
  }

  private class Host(val address: String, val name: String) {
    override fun toString() = if (name.isEmpty() || name == address) address else "$address ( $name )"
  }

  private class HostsList(label: String, selectedHost: String?) : JPanel() {
    private val title = JLabel(label)
    private val hosts: JComboBox<Host> = ComboBox<Host>().apply {
      val hosts = listOf(ALL_HOSTS) + getHostList()
      hosts.forEach(::addItem)
      selectedHost?.let { selectedHost ->
        selectedItem = hosts.find { it.address == selectedHost }
      }

      addActionListener { onChange?.invoke() }
    }

    fun clear() = hosts.removeAllItems()

    fun addItems(values: List<Host>) = values.forEach { hosts.addItem(it) }

    fun setItems(values: List<Host>) {
      clear()
      addItems(values)
    }

    fun setSelectedItem(host: Host?) {
      hosts.selectedItem = host
    }

    init {
      LinearPanelBuilder(this)
        .addNextComponent(title, weightx = 0.1)
        .addNextComponent(hosts)
    }

    var onChange: (() -> Unit)? = null

    val selected get() = hosts.selectedItem as? Host

    override fun setEnabled(enabled: Boolean) {
      super.setEnabled(enabled)
      hosts.isEnabled = enabled
    }

    fun setTooltip(text: String?) {
      hosts.toolTipText = text
    }
  }

  private class PortEditor(portValue: String?) : JPanel() {
    private val title = JLabel("Port:")
    private val port: JTextField = JTextField().apply {
      text = portValue?.takeIf(String::isNotEmpty) ?: ProjectorServer.getEnvPort().toString()

      addKeyListener(object : KeyAdapter() {
        override fun keyReleased(e: KeyEvent) {
          onChange?.invoke()
        }
      })
    }

    init {
      LinearPanelBuilder(this)
        .addNextComponent(title, weightx = 0.1)
        .addNextComponent(port)
    }

    var onChange: (() -> Unit)? = null
    val value get() = port.text ?: ""

    override fun setEnabled(enabled: Boolean) {
      super.setEnabled(enabled)
      port.isEnabled = enabled
    }
  }

  companion object {
    private val ALL_HOSTS = Host("0.0.0.0", "all addresses")
    private val dockerVendor = byteArrayOf(0x02.toByte(), 0x42.toByte())

    private fun generatePassword(): String {
      val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
      return (1..11)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
    }

    private fun toHost(ip: java.net.InetAddress): Host {
      var ipString = ip.toString().substring(1)

      if (ip is Inet6Address) {
        ipString = "[${ipString.substring(0, ipString.indexOf('%'))}]"
      }

      return Host(ipString, getHostName(ip) ?: "")
    }

    private fun getHostList() = NetworkInterface.getNetworkInterfaces()
      .asSequence()
      .filterNotNull()
      .filterNot {
        it.hardwareAddress != null && it.hardwareAddress.sliceArray(0..1).contentEquals(dockerVendor)
      } // drop docker
      .flatMap { it.interfaceAddresses?.asSequence()?.filterNotNull() ?: emptySequence() }
      .filterNot { it.address is Inet6Address } // drop IP v 6
      .map { toHost(it.address) }
      .toList()
  }
}
