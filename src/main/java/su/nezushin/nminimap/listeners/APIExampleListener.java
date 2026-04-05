package su.nezushin.nminimap.listeners;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.map.MapPalette;
import su.nezushin.nminimap.api.events.AsyncMapRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.markers.NMapMarker;
import su.nezushin.nminimap.markers.impl.LocationMarker;
import su.nezushin.nminimap.markers.impl.PositionMarker;
import su.nezushin.nminimap.util.ColorUtil;

import java.util.List;

public class APIExampleListener implements Listener {

    @EventHandler
    public void drawDot(AsyncMapRenderEvent event){
        byte[] mapData = event.getMapData();

        //middle of the map
        int x = 64;
        int y = 64;

        //Accepts only colors from ColorUtil.colors. Another values will result transparent color
        mapData[x + (y * 128)] = ColorUtil.exactColor(ColorUtil.colors[10]);


        int anotherX = 1;//first line is reserved for internal use. Displayed map has resolution 127 x 128
        int anotherY = 0;

        //Similar to deprecated MapPalette.matchColor(). Will find most nearest color.
        mapData[anotherX + (anotherY * 128)] = ColorUtil.getNearestColor(Color.fromRGB(255, 0, 0));
    }

    @EventHandler
    public void drawMarker(AsyncMarkerRenderEvent e){
        List<NMapMarker> markers = e.getMarkers();

        String markerIcon = "player";//Icon from NMinimap/markers directory

        //Add marker with fixed location
        markers.add(new LocationMarker(markerIcon, new Location(e.getPlayer().getPlayer().getWorld(), 1, 1, 1)));

        int positionMarkerX = 0;//Accepts values from -127 to 127. -127 - left. 127 - right
        int positionMarkerY = 0;//-127 - top. 127 - bottom
        int positionMarkerRotation = 0;//from 0 to 256. 0 points top, 128 points bottom

        //Add marker with relative position on map.
        markers.add(new PositionMarker(markerIcon, positionMarkerX, positionMarkerY, positionMarkerRotation));

    }
}
