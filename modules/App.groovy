import spaceport.Spaceport
import spaceport.computer.alerts.Alert
import spaceport.computer.alerts.results.HttpResult
import spaceport.computer.alerts.results.Result
import spaceport.computer.memory.physical.Document
import spaceport.launchpad.Launchpad

class App {


    static Launchpad launchpad = new Launchpad()


    // Home page (About the game, etc.)

    @Alert('on / hit')
    static _home(HttpResult r) {
        launchpad.assemble(['index.ghtml']).launch(r, '_wrapper.ghtml')
    }


    // Init stuff
    @Alert('on initialized')
    static _init(Result r) {
        Spaceport.main_memory_core.createDatabaseIfNotExists('worlds')
    }


    // World page (Play in a world)
    @Alert('~on /w/([^/]+) hit')
    static _world(HttpResult r) {
        def world = Document.get(r.matches[0].clean().slugify(), 'worlds')
        r.context.data.'world' = world
        launchpad.assemble(['world/view.ghtml']).launch(r, '_wrapper.ghtml')
    }

}