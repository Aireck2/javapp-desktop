package com.app.models;

import java.util.List;

/** View data for one student's attendance in a course session. */
public final class StudentAttendanceModel {

  private final String index;
  private final String id;
  private final String name;
  private final String code;
  private final List<HourBlock> blocks;
  private final int prog;
  private final int pres;
  private final int aus;
  private final int pctAsis;
  private final int pctInas;
  private final String note;

  public StudentAttendanceModel(
      String index,
      String id,
      String name,
      String code,
      List<HourBlock> blocks,
      int prog,
      int pres,
      int aus,
      int pctAsis,
      int pctInas,
      String note) {
    this.index = index;
    this.id = id;
    this.name = name;
    this.code = code;
    this.blocks = List.copyOf(blocks);
    this.prog = prog;
    this.pres = pres;
    this.aus = aus;
    this.pctAsis = pctAsis;
    this.pctInas = pctInas;
    this.note = note;
  }

  public String index() {
    return index;
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String code() {
    return code;
  }

  public List<HourBlock> blocks() {
    return blocks;
  }

  public int prog() {
    return prog;
  }

  public int pres() {
    return pres;
  }

  public int aus() {
    return aus;
  }

  public int pctAsis() {
    return pctAsis;
  }

  public int pctInas() {
    return pctInas;
  }

  public String note() {
    return note;
  }

  public boolean isExempt() {
    return note != null && !note.isBlank();
  }

  public boolean isPresent() {
    return !blocks.isEmpty() && blocks.stream().allMatch(HourBlock::isChecked);
  }

  public boolean isAbsent() {
    return blocks.isEmpty() || blocks.stream().noneMatch(HourBlock::isChecked);
  }

  public void setAllChecked(boolean checked) {
    blocks.forEach(block -> block.setChecked(checked));
  }

  public boolean[] attendanceMarks() {
    boolean[] marks = new boolean[blocks.size()];
    for (int i = 0; i < blocks.size(); i++) {
      marks[i] = blocks.get(i).isChecked();
    }
    return marks;
  }

  public static final class HourBlock {
    private final String time;
    private boolean checked;

    public HourBlock(String time, boolean checked) {
      this.time = time;
      this.checked = checked;
    }

    public String time() {
      return time;
    }

    public boolean isChecked() {
      return checked;
    }

    public void setChecked(boolean checked) {
      this.checked = checked;
    }
  }
}
