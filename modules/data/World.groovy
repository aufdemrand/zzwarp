package data

import spaceport.computer.memory.physical.Document

class World extends Document {


    // Blocks are the items that make up the board of the world
    static class Block {
        String color = '#000000'
        List    grid = []
    }


    // Palette is the colors of the world
    static class Palette {
        // Use 0-15 as the keys
        Map colors = [
            '0' : '#0a0a0c',   // void black
            '1' : '#1a1a2e',   // deep navy
            '2' : '#2b1055',   // dark purple
            '3' : '#0f3460',   // midnight blue
            '4' : '#39ff14',   // neon green
            '5' : '#00ffff',   // cyan
            '6' : '#bf00ff',   // magenta
            '7' : '#ff006e',   // hot pink
            '8' : '#ff6600',   // orange
            '9' : '#ffee00',   // yellow
            '10': '#00ff88',   // mint
            '11': '#4466ff',   // electric blue
            '12': '#ff4444',   // red
            '13': '#555577',   // stone gray
            '14': '#aaaacc',   // light gray
            '15': '#ffffff',   // white
        ]
    }


    // Meeples are the 'inhabitants' of the world
    // x, y are pixel-grid coordinates (0-287), anchored to bottom-left
    static class Meeple {
        Integer     x = 0
        Integer     y = 0
        String   size = '1x1'
        String  color = '#FF00FF'
        List thoughts = []
        Integer  mood = 0
        Long birth = 0
        String   name = ''
    }


    // World properties

    def customProperties = [ 'name', 'board', 'meeples', 'tiles', 'totalBorn', 'timer', 'palette' ]

    String name = 'New World'
    List<Block> board = buildDefaultBoard()

    static List<Block> buildDefaultBoard() {
        def blocks = (0..<64).collect { new Block() }

        // 0: Void (default black, empty grid)

        // 1: Grass
        blocks[1] = new Block(color: '#39ff14', grid: [
            4, 4,10, 4, 4, 4,10, 4,
            4,10, 4, 4,10, 4, 4, 4,
            4, 4, 4,10, 4, 4, 4,10,
           10, 4, 4, 4, 4,10, 4, 4,
            4, 4,10, 4, 4, 4, 4, 4,
            4,10, 4, 4, 4, 4,10, 4,
            4, 4, 4, 4,10, 4, 4, 4,
           10, 4, 4,10, 4, 4, 4,10,
        ])

        // 2: Stone
        blocks[2] = new Block(color: '#555577', grid: [
           13,13,14,13,13,13,13,14,
           13,14,13,13,14,13,14,13,
           14,13,13,13,13,14,13,13,
           13,13,14,14,13,13,13,14,
           13,14,13,13,13,14,13,13,
           14,13,13,14,13,13,14,13,
           13,13,14,13,14,13,13,13,
           13,14,13,13,13,13,14,13,
        ])

        // 3: Water
        blocks[3] = new Block(color: '#0f3460', grid: [
            3, 3, 5, 3, 3, 3, 3, 5,
            3, 5, 3, 3, 5, 3, 5, 3,
            3, 3, 3, 5, 3, 3, 3, 3,
            5, 3, 3, 3, 3, 5, 3, 3,
            3, 3, 5, 3, 5, 3, 3, 5,
            3, 5, 3, 3, 3, 3, 5, 3,
            5, 3, 3, 5, 3, 3, 3, 3,
            3, 3, 5, 3, 3, 5, 3, 5,
        ])

        // 4: Brick
        blocks[4] = new Block(color: '#ff4444', grid: [
           12,12,12,13,12,12,12,12,
           12,12,12,13,12,12,12,12,
           13,13,13,13,13,13,13,13,
           12,12,12,12,12,13,12,12,
           12,12,12,12,12,13,12,12,
           13,13,13,13,13,13,13,13,
           12,12,12,13,12,12,12,12,
           12,12,12,13,12,12,12,12,
        ])

        // 5: Sand
        blocks[5] = new Block(color: '#ff6600', grid: [
            8, 8, 9, 8, 8, 8, 9, 8,
            8, 9, 8, 8, 9, 8, 8, 8,
            8, 8, 8, 9, 8, 8, 8, 9,
            9, 8, 8, 8, 8, 9, 8, 8,
            8, 8, 9, 8, 8, 8, 8, 8,
            8, 9, 8, 8, 8, 8, 9, 8,
            8, 8, 8, 8, 9, 8, 8, 8,
            9, 8, 8, 9, 8, 8, 8, 9,
        ])

        // 6: Dark earth
        blocks[6] = new Block(color: '#1a1a2e', grid: [
            1, 1, 2, 1, 1, 1, 1, 2,
            1, 2, 1, 1, 2, 1, 2, 1,
            2, 1, 1, 1, 1, 2, 1, 1,
            1, 1, 1, 2, 1, 1, 1, 2,
            1, 2, 1, 1, 1, 2, 1, 1,
            2, 1, 1, 2, 1, 1, 2, 1,
            1, 1, 2, 1, 1, 1, 1, 1,
            1, 2, 1, 1, 2, 1, 1, 2,
        ])

        // 7: Glitch/neon
        blocks[7] = new Block(color: '#2b1055', grid: [
            6, 0, 6, 0, 7, 0, 7, 0,
            0, 6, 0, 7, 0, 7, 0, 6,
            6, 0, 7, 0, 6, 0, 6, 0,
            0, 7, 0, 6, 0, 6, 0, 7,
            7, 0, 6, 0, 7, 0, 7, 0,
            0, 6, 0, 7, 0, 6, 0, 6,
            7, 0, 7, 0, 6, 0, 6, 0,
            0, 7, 0, 6, 0, 7, 0, 7,
        ])

        return blocks
    }
    List<Integer> tiles = (0..<1296).collect { 0 }  // 36x36 tile map, each entry is a block index (0-63)
    List<Meeple> meeples = buildDefaultMeeples()

    static List<Meeple> buildDefaultMeeples() {
        def rand = new Random()
        def neutralColors = ['#aaaacc', '#cccccc', '#8888aa', '#bbbbdd', '#9999bb']
        return (0..<5).collect { i ->
            def size = randomSize(rand)
            def (sw, sh) = size.split('x').collect { it as int }
            new Meeple(
                x: rand.nextInt(288 - sw),
                y: rand.nextInt(288 - sh),
                size: size,
                color: neutralColors[i],
                mood: 20 + rand.nextInt(40),
                name: generateName(rand),
                birth: System.currentTimeMillis(),
            )
        }
    }
    static final String[] SPAWN_COLORS = [
        '#aaaacc', '#cccccc', '#8888aa', '#bbbbdd', '#9999bb',
        '#aa99bb', '#99aabb', '#bbaacc', '#ccbbaa', '#aabbcc',
    ]

    static String randomSize(Random rng) {
        def roll = rng.nextInt(100)
        if (roll < 2)  return '3x3'   // 2%
        if (roll < 6)  return '2x3'   // 4%
        if (roll < 30) return '2x2'   // 24%
        if (roll < 60) return '1x2'   // 30%
        return '1x1'                  // 40%
    }

    static Meeple spawnMeeple(Random rng) {
        def size = randomSize(rng)
        def (sw, sh) = size.split('x').collect { it as int }
        new Meeple(
            x: rng.nextInt(288 - sw),
            y: rng.nextInt(288 - sh),
            size: size,
            color: SPAWN_COLORS[rng.nextInt(SPAWN_COLORS.length)],
            mood: 20 + rng.nextInt(40),
            name: generateName(rng),
            birth: System.currentTimeMillis(),
        )
    }

    Palette palette = new Palette()

    Long lastSpawn = 0
    Integer totalBorn = 5  // initial 5 from buildDefaultMeeples

    String currentPlayer
    List spectators = []
    Integer timer = 60
    Integer tickCount = 0
    // Worlds will stop 'ticking' after 30 minutes of no updates
    Long lastUpdate = System.currentTimeMillis()

    // Game Tick!!

    // Geometric glyph alphabet for meeple names
    static final String[] GLYPHS = [
        '△', '▽', '○', '□', '◇', '☆', '●', '■',
        '◆', '▲', '▼', '◈', '⬡', '◎', '▢', '⏣',
    ]

    static String generateName(Random rng) {
        def len = 2 + rng.nextInt(4)  // 2-5 characters
        return (0..<len).collect { GLYPHS[rng.nextInt(GLYPHS.length)] }.join('')
    }

    static final Random tickRng = new Random()

    def tick() {
        long now = System.currentTimeMillis()
        def changed = false
        def dead = []

        for (m in meeples) {
            // Backfill name/birth for existing meeples
            if (!m.name) m.name = generateName(tickRng)
            if (!m.birth) m.birth = now

            // Age in game years (1 real day = 1 game year)
            def ageYears = (now - m.birth) / 86400000.0

            // Death: small chance always, escalates with age
            if (ageYears >= 100 && tickRng.nextInt(300000) == 0) {
                dead << m; continue    // ~94% per game year
            } else if (ageYears >= 75 && tickRng.nextInt(1000000) == 0) {
                dead << m; continue    // ~58% per game year
            } else if (ageYears >= 1 && tickRng.nextInt(5000000) == 0) {
                dead << m; continue    // ~16% per game year
            }

            // Mood drift: random walk ±2 per tick, clamped 0-128
            m.mood = Math.max(0, Math.min(128, m.mood + tickRng.nextInt(5) - 2))

            // Movement chance: mood²/80000 (mood 40 ≈ 2%, mood 80 ≈ 8%, mood 128 ≈ 20%)
            def moveChance = m.mood * m.mood
            if (tickRng.nextInt(80000) >= moveChance) continue

            def (sw, sh) = m.size.split('x').collect { it as int }
            def steps = 1 + (m.mood >> 5)             // 1-4 px per move

            // 40% chance to move toward nearest meeple (congregation)
            def dir
            if (tickRng.nextInt(100) < 40 && meeples.size() > 1) {
                def nearest = null
                def bestDist = Integer.MAX_VALUE
                for (other in meeples) {
                    if (other.is(m)) continue
                    def dist = Math.abs(other.x - m.x) + Math.abs(other.y - m.y)
                    if (dist < bestDist) { bestDist = dist; nearest = other }
                }
                if (nearest) {
                    def dx = nearest.x - m.x
                    def dy = nearest.y - m.y
                    if (Math.abs(dx) >= Math.abs(dy)) {
                        dir = dx > 0 ? 3 : 2
                    } else {
                        dir = dy > 0 ? 0 : 1
                    }
                } else {
                    dir = tickRng.nextInt(4)
                }
            } else {
                dir = tickRng.nextInt(4)
            }

            switch (dir) {
                case 0: m.y = Math.min(m.y + steps, 287 - sh); break
                case 1: m.y = Math.max(m.y - steps, 0);        break
                case 2: m.x = Math.max(m.x - steps, 0);        break
                case 3: m.x = Math.min(m.x + steps, 287 - sw); break
            }
            changed = true
        }

        // Remove dead meeples
        if (dead) {
            meeples.removeAll(dead)
            changed = true
        }

        // Spawn new meeples based on population
        // Early game burst: 1 every 30s for the first 5 minutes
        def pop = meeples.size()
        def earliest = meeples.collect { it.birth ?: now }.min() ?: now
        def worldAge = now - earliest
        long spawnInterval = worldAge < 300000L ? 30000L : (pop < 50 ? 600000L : (pop < 128 ? 3600000L : (pop < 256 ? 86400000L : 0L)))
        if (spawnInterval > 0 && now - (lastSpawn ?: 0) >= spawnInterval) {
            meeples.add(spawnMeeple(tickRng))
            totalBorn = (totalBorn ?: meeples.size()) + 1
            lastSpawn = now
            changed = true
        }

        // Timer countdown: decrement once per second (every 10 ticks)
        tickCount = (tickCount + 1) % 10
        if (tickCount == 0 && timer > 0) {
            timer--
            changed = true
        }

        if (changed) this._update()
    }


    // Supertick: condensed simulation for idle worlds
    def supertick(long elapsedMs) {
        long ticks = elapsedMs / 100
        long now = System.currentTimeMillis()
        def dead = []

        for (m in meeples) {
            if (!m.name) m.name = generateName(tickRng)
            if (!m.birth) m.birth = now

            // Mood settles to a random value over long idle periods
            m.mood = tickRng.nextInt(129)

            // Death probability over N ticks: P(die) = 1 - (1 - p)^N
            def ageYears = (now - m.birth) / 86400000.0
            double deathProb
            if (ageYears >= 100) {
                deathProb = 1.0 - Math.pow(1.0 - 1.0 / 300000, ticks)
            } else if (ageYears >= 75) {
                deathProb = 1.0 - Math.pow(1.0 - 1.0 / 1000000, ticks)
            } else if (ageYears >= 1) {
                deathProb = 1.0 - Math.pow(1.0 - 1.0 / 5000000, ticks)
            } else {
                deathProb = 0
            }
            if (tickRng.nextDouble() < deathProb) {
                dead << m; continue
            }

            // Movement: scatter position randomly within a range
            def (sw, sh) = m.size.split('x').collect { it as int }
            def drift = Math.min(40, (int) Math.sqrt(ticks) / 4)
            m.x = Math.max(0, Math.min(287 - sw, m.x + tickRng.nextInt(drift * 2 + 1) - drift))
            m.y = Math.max(0, Math.min(287 - sh, m.y + tickRng.nextInt(drift * 2 + 1) - drift))
        }

        if (dead) meeples.removeAll(dead)

        // Spawning: calculate how many should have appeared
        def pop = meeples.size()
        def earliest = meeples.collect { it.birth ?: now }.min() ?: now
        def worldAge = now - earliest
        long spawnInterval = worldAge < 300000L ? 30000L : (pop < 50 ? 600000L : (pop < 128 ? 3600000L : (pop < 256 ? 86400000L : 0L)))
        if (spawnInterval > 0) {
            int spawns = (int) (elapsedMs / spawnInterval)
            spawns.times {
                if (meeples.size() < 256) {
                    meeples.add(spawnMeeple(tickRng))
                    totalBorn = (totalBorn ?: meeples.size()) + 1
                }
            }
        }
        lastSpawn = now

        println "[WORLD] Supertick — ${ticks} equivalent ticks, ${dead.size()} died, pop now ${meeples.size()}"
        this._update()
    }


    // Business Logic

    def setPaletteColor(def index, def color) {
        palette.colors."${ index }" = color
        this._update()
        save()
    }
}