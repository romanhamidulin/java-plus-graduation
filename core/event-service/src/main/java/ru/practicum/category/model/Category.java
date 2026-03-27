package ru.practicum.category.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.events.model.Event;

import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, unique = true, length = 50)
    private String name;

    @OneToMany(mappedBy = "category")
    Set<Event> events;
}
