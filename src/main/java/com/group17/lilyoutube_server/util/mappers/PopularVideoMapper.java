package com.group17.lilyoutube_server.util.mappers;

import com.group17.lilyoutube_server.dto.PopularVideoDTO;
import com.group17.lilyoutube_server.model.PopularVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PostMapper.class})
public interface PopularVideoMapper {
    PopularVideoDTO toDto(PopularVideo popularVideo);
    PopularVideo toEntity(PopularVideoDTO popularVideoDTO);
}
