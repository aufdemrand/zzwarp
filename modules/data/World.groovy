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
        Map colors = [:]
    }


    // Meeples are the 'inhabitants' of the world
    // x, y are pixel-grid coordinates (0-383), anchored to bottom-left
    static class Meeple {
        Integer     x = 0
        Integer     y = 0
        String   size = '1x1'
        String  color = '#FF00FF'
        List thoughts = []
        Integer  mood = 0
    }


    // World properties

    def customProperties = [ 'name', 'board', 'meeples' ]

    String name = 'New World'
    List<Block> board = 64.collect { new Block() }
    List<Meeple> meeples = []
    Palette palette = new Palette()

}