package game

import data.World
import spaceport.computer.alerts.Alert
import spaceport.computer.alerts.results.Result
import spaceport.computer.memory.physical.Document
import spaceport.computer.memory.virtual.Cargo
import spaceport.personnel.Client

class Game {


    static Game currentGame


    @Alert('on initialized')
    static _gameStart(Result r) {
        currentGame = new Game()
    }

    @Alert('on deinitialized')
    static _hotReload(Result r) {
        currentGame.stop()
    }


    Thread gameThread
    volatile boolean running = false

    static final long TICK_MS = 100                    // 10 ticks per second
    static final long IDLE_MS = 30 * 60 * 1000        // 30 minutes → switch to supertick
    static final long SAVE_MS = 5 * 60 * 1000         // 5 minutes
    static final long SUPERTICK_MS = 60 * 60 * 1000   // 1 hour between superticks

    Map<String, Long> lastSaved = [:]
    Map<String, Long> lastSuperticked = [:]


    Game() {
        println "[GAME] Starting game loop (${TICK_MS}ms tick)"
        running = true
        gameThread = Thread.start {
            while (running) {
                try {
                    tickAll()
                } catch (Exception e) {
                    e.printStackTrace()
                }
                Thread.sleep(TICK_MS)
            }
            println '[GAME] Loop stopped'
        }
    }


    void tickAll() {
        def activeGames = getActiveGames()
        if (!activeGames) return

        long now = System.currentTimeMillis()

        activeGames.each { worldId ->
            def world = Document.get(worldId, 'worlds') as World
            if (!world) return

            long idle = now - (world.lastUpdate ?: 0)

            if (idle <= IDLE_MS) {
                // Active: normal tick
                world.tick()

                // God rotation when timer expires
                if (world.timer <= 0) {
                    rotateGod(world)
                    world.timer = 60
                    world._update()
                }

                long savedAt = lastSaved.get(worldId, 0L)
                if (now - savedAt >= SAVE_MS) {
                    println "[GAME] Saving world '${worldId}'"
                    world.save()
                    lastSaved[worldId] = now
                }
            } else {
                // Idle: supertick once per hour
                long lastSuper = lastSuperticked.get(worldId, 0L)
                if (lastSuper == 0L) lastSuper = now  // first idle tick, start tracking
                long elapsed = now - lastSuper
                if (elapsed >= SUPERTICK_MS) {
                    println "[GAME] Supertick world '${worldId}' — ${(elapsed / 3600000).round(1)}h elapsed"
                    world.supertick(elapsed)
                    world.save()
                    lastSuperticked[worldId] = now
                    lastSaved[worldId] = now
                }
            }
        }
    }


    static getActiveGames() {
        return Cargo.fromStore('game').getList('active-games')
    }

    static setActive(String world) {
        println "[GAME] Activating world '${world}'"
        Cargo.fromStore('game').addToSet('active-games', world)
    }

    static final Random rng = new Random()

    static void rotateGod(World world) {
        // Clean spectators: keep only unique, active connections
        def active = (world.spectators ?: []).unique().findAll { uuid ->
            try {
                def c = Client.getClientByCookie(uuid)
                return c != null && c.isActive()
            } catch (Exception e) { return false }
        }
        world.spectators = active

        if (active.isEmpty()) {
            world.currentPlayer = null
            println "[GAME] No active spectators — no god assigned"
            return
        }

        // Pick a random active spectator (prefer someone who isn't already god)
        def candidates = active.findAll { it != world.currentPlayer }
        if (candidates.isEmpty()) {
            // Only one spectator and they're already god — make them wait a round
            world.currentPlayer = null
            println "[GAME] Solo spectator must wait a round — no god assigned"
            return
        }

        def chosen = candidates[rng.nextInt(candidates.size())]
        world.currentPlayer = chosen
        println "[GAME] New god: ${chosen} (from ${active.size()} active spectators)"
    }

    void stop() {
        println '[GAME] Stopping game loop...'
        running = false
        gameThread?.join(1000)
    }

}
