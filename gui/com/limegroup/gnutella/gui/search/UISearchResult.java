/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.limegroup.gnutella.gui.search;

import javax.swing.JPopupMenu;

import com.frostwire.bittorrent.websearch.WebSearchResult;
import com.frostwire.search.SearchResult;

/**
 * A single SearchResult. These are returned in the {@link SearchInputPanel} and
 * are used to create {@link SearchResultDataLine}s to show search results. *
 */
public interface UISearchResult {

    /**
     * @return the file name
     */
    String getFilename();
    
    /**
     * Gets the size of this SearchResult.
     */
    long getSize();
    
    /**
     * @return milliseconds since January 01, 1970 the artifact of t
     */
    long getCreationTime();
    
    public String getSource();
    
    /**
     * Returns the extension of this result.
     */
    String getExtension();

    public void download(boolean partial);

    JPopupMenu createMenu(JPopupMenu popupMenu, SearchResultDataLine[] lines, SearchResultMediator rp);
    
    public String getHash();

    public String getTorrentURI();

    public int getSeeds();
    
    public SearchEngine getSearchEngine();
    
    public SearchResult getSearchResult();
    
    public void showDetails(boolean now);
    
    public String getDisplayName();
    
    public boolean allowDeepSearch();
    
    public String getQuery();
    
    public void play();
}