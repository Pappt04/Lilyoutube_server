package com.group17.lilyoutube_server.util.mappers;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {
    @Mapping(source = "user.id", target = "user_id")
    PostDTO toDto(Post post);

    @Mapping(source = "user_id", target = "user.id")
    Post toEntity(PostDTO postDTO);
}
