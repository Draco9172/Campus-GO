package com.campusgo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private String name;

    private String description;

    private String category;

    @Column(name = "price_paise", nullable = false)
    private Integer pricePaise;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    public MenuItem() {
    }

    public MenuItem(Vendor vendor, String name, Integer pricePaise, String category) {
        this.vendor = vendor;
        this.name = name;
        this.pricePaise = pricePaise;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPricePaise() {
        return pricePaise;
    }

    public void setPricePaise(Integer pricePaise) {
        this.pricePaise = pricePaise;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
