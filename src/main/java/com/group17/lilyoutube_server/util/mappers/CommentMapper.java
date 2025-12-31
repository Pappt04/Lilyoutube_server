package com.group17.lilyoutube_server.util.mappers;

import org.springframework.stereotype.Component;
import com.group17.lilyoutube_server.dto.CommentDTO;
import com.group17.lilyoutube_server.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(source = "user.id", target = "user_id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "post.id", target = "post_id")
    CommentDTO toDto(Comment comment);
    
    Comment toEntity(CommentDTO comentDto);
}
