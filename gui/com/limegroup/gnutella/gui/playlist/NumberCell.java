package com.limegroup.gnutella.gui.playlist;

/**
 *  Empty place holder for a table cell. This class is linked to the NumberTableCellRenderer
 *  which writes the table row that this cell is in. The renderer dynamically looks up
 *  what row this cell is currently in so no information is needed to be passed in
 *  to the constructor of this class.
 */
class NumberCell {
    private boolean _playing;
    
    public boolean isPlaying() {
        return _playing;
    }
    
    public void setPlaying(boolean playing) {
        _playing = playing;
    }
}
