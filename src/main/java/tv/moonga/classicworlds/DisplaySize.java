package tv.moonga.classicworlds;

public enum DisplaySize {
  NORMAL("Normal",1),LARGER("Larger",1.15),EXTRA_LARGE("Extra Large",1.3);
  final double factor;
  private final String label;
  DisplaySize(String label,double factor){this.label=label;this.factor=factor;}
  @Override public String toString(){return label;}
}
