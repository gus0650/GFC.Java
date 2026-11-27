package gfc.microedition.media;

public interface Controllable {
	Control getControl(String controlType);
	Control[] getControls();
}
