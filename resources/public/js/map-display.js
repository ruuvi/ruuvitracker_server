var map;

var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
var maanmittausUrl='http://tiles.kartat.kapsi.fi/peruskartta/{z}/{x}/{y}.jpg';

var osmAttribution = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>';
var maanmittausAttribution = 'Maanmittauslaitos peruskartta';

function initMap(mapCanvasId, url, attribution) {
    var startLocation = new L.LatLng(60.168564, 24.941111);
    // initialize the map on the "map" div
    map = new L.Map(mapCanvasId);    

    //var url = osmUrl;
    // create a CloudMade tile layer (or use other provider of your choice)
    var mapTiles = new L.TileLayer(url, {
	'attribution': attribution,
	'maxZoom': 18
    });
    
    // add the CloudMade layer to the map set the view to a given center and zoom
    map.addLayer(mapTiles).setView(startLocation, 13);
    
    // create a marker in the given location and add it to the map
    addMarker(map, startLocation, "T&auml;m&auml; on aloituspiste");
    addMarker(map, new L.LatLng(60.187857, 24.935703), "T&auml;m&auml; on <a href='http://linnanmaki.fi/'>Linnanm&auml;ki</a>. 60.187857, 24.935703");
    addMarker(map, new L.LatLng(60.155945, 24.921412));
    
}

function addMarker(map, position, popupText) {
    var marker = new L.Marker(position);
    map.addLayer(marker);
    if(popupText) {
	marker.bindPopup(popupText);
    }
}
function centerMap(map, latitude, longitude) {
    position = new L.LatLng(60.187857, 24.935703);
}

function locateMe() {
   if(navigator.geolocation) {
       map.locateAndSetView(13);
       var position = map.getCenter();       
       addMarker(map, position, "My current position");
   }
}