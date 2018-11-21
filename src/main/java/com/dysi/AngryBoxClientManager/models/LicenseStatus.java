package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "LICENSESTATUS")
public class LicenseStatus {
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // fecha baja
    @Column(name = "REMOVEDATE")
    private Calendar removeDate;
    
    // fecha cracion
    @Column(name = "CREATIONDATE")
    private Calendar creationDate;
    
    @Column(name = "OBSERVATION")
    private String observation;
    
    @Column(name = "LIMITTRANSACTION")
    private Long limitTransaction;
    
    // blockeo forzado
    @Column(name = "FORCEDLOCK")
    private boolean forcedLock;
    
    // fecha bloqueo
    @Column(name = "LOCKDATE")
    private Calendar lockDate;

    @ManyToOne
    @Column(name = "LICENSE")
    private License license;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Calendar getRemoveDate() {
        return removeDate;
    }

    public void setRemoveDate(Calendar removeDate) {
        this.removeDate = removeDate;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public Long getLimitTransaction() {
        return limitTransaction;
    }

    public void setLimitTransaction(Long limitTransaction) {
        this.limitTransaction = limitTransaction;
    }

    @JsonIgnore
    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public boolean isForcedLock() {
        return forcedLock;
    }

    public void setForcedLock(boolean forcedLock) {
        this.forcedLock = forcedLock;
    }

    public Calendar getLockDate() {
        return lockDate;
    }

    public void setLockDate(Calendar lockDate) {
        this.lockDate = lockDate;
    }
}
