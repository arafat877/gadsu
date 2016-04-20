package at.cpickl.gadsu.view

import at.cpickl.gadsu.image.ImagePicker

object ViewNames {
    val Main = MainViewNames
    val MenuBar = MenuBarViewNames
    val Client = ClientViewNames
    val Treatment = TreatmentViewNames
    val Preferences = PreferencesViewNames
}

object MainViewNames {
    val ContainerPanel = "Main.ContainerPanel"
    val ContentPanel = "Main.ContentPanel"
}

object MenuBarViewNames {
    val ProtocolGenerate = "MenuBar.ProtocolGenerate"
}

object ClientViewNames {
    val MainPanel = "Client.MainPanel"
    val List = "Client.List"

    val CreateButton = "Client.CreateButton"
    val SaveButton = "Client.SaveButton"
    val CancelButton = "Client.CancelButton"

    val InputFirstName = "Client.InputFirstName"
    val InputLastName = "Client.InputLastName"

    val ImageContainer = "Client.ImageContainer"
    val ImagePrefix = "Client.Image" // .Panel, .OpenButton
    val OpenImageButton = "${ImagePrefix}.${ImagePicker.VIEWNAME_SUFFIX_OPENBUTTON}"
    val ImagePickerPanel = "${ImagePrefix}.${ImagePicker.VIEWNAME_SUFFIX_PANEL}"
    val TabbedPane = "Client.TabbedPane"
}

object TreatmentViewNames {
    val MainPanel = "Treatment.MainPanel"
    val OpenNewButton = "Treatment.OpenNewButton"
    val BackButton = "Treatment.BackButton"
    val SaveButton = "Treatment.SaveButton"
    val ListInClientView = "Treatment.ListInClientView"
    val InputNote = "Treatment.InputNote"
}

object PreferencesViewNames {
    val Window = "Preferences.Window"
}
