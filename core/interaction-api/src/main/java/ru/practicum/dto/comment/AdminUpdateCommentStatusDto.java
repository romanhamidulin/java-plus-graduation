package ru.practicum.dto.comment;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AdminUpdateCommentStatusDto {
    private AdminUpdateCommentStatusAction action;
}
