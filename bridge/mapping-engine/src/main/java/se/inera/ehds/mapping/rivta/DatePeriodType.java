package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatePeriodType", propOrder = {"start", "end"})
public class DatePeriodType {
    private String start;
    private String end;

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
}
