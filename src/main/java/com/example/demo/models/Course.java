package com.example.demo.models;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    // @Temporal required for validation
    // @DateTimeFormat will show the date as given, but ONLY when being pulled out of db, will still be stored
    // as full java.util.Date, which is what I want, for consistency
    @NotNull
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "MMM d, yyyy")
    private Date dateStart;

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "MMM d, yyyy")
    private Date dateEnd;

    private long courseRegistrationNum;

    @NotEmpty
    private String name;

    private long numEvaluationsCompleted;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<RegistrationTimestamp> timeStamps;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<Evaluation> evaluations;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<Attendance> attendances;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "person_id"))
    private Collection<Person> persons;


    public Course(Collection<RegistrationTimestamp> timeStamps, Collection<Evaluation> evaluations, Collection<Attendance> attendances, Collection<Person> persons) {
        this.timeStamps = new HashSet<>();
        this.evaluations = new HashSet<>();
        this.attendances = new HashSet<>();
        this.persons = new HashSet<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public long getCourseRegistrationNum() {
        return courseRegistrationNum;
    }

    public void setCourseRegistrationNum(long courseRegistrationNum) {
        this.courseRegistrationNum = courseRegistrationNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNumEvaluationsCompleted() {
        return numEvaluationsCompleted;
    }

    public void setNumEvaluationsCompleted(long numEvaluationsCompleted) {
        this.numEvaluationsCompleted = numEvaluationsCompleted;
    }

    public Collection<RegistrationTimestamp> getTimeStamps() {
        return timeStamps;
    }

    public void setTimeStamps(Collection<RegistrationTimestamp> timeStamps) {
        this.timeStamps = timeStamps;
    }

    public Collection<Evaluation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(Collection<Evaluation> evaluations) {
        this.evaluations = evaluations;
    }

    public Collection<Attendance> getAttendances() {
        return attendances;
    }

    public void setAttendances(Collection<Attendance> attendances) {
        this.attendances = attendances;
    }

    public Collection<Person> getPersons() {
        return persons;
    }

    public void setPersons(Collection<Person> persons) {
        this.persons = persons;
    }
}
