package edu.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String author;
    private String category;
    private int totalQuantity;
    private int availableQuantity;

    public Book() {}
    public Book(String title, String author, String category, int totalQuantity) {
        this.title=title; this.author=author; this.category=category;
        this.totalQuantity=totalQuantity; this.availableQuantity=totalQuantity;
    }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getAuthor(){return author;} public void setAuthor(String author){this.author=author;}
    public String getCategory(){return category;} public void setCategory(String category){this.category=category;}
    public int getTotalQuantity(){return totalQuantity;} public void setTotalQuantity(int q){this.totalQuantity=q;}
    public int getAvailableQuantity(){return availableQuantity;} public void setAvailableQuantity(int q){this.availableQuantity=q;}
}
