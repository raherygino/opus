import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Star,
  Clock,
  MoreHorizontal,
  Trash2,
  Pin,
  Tag,
} from "lucide-react";

interface NoteEditorProps {
  noteId?: string;
}

export function NoteEditor({ noteId }: NoteEditorProps) {
  const [title, setTitle] = useState(noteId ? "Meeting Notes" : "Untitled");
  const [content, setContent] = useState(
    noteId
      ? "Discussed Q4 goals and project timelines..."
      : "",
  );

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b border-border px-6 py-3">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <Star className="h-4 w-4" />
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <Pin className="h-4 w-4" />
          </Button>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <Clock className="h-3 w-3" />
            <span>Edited 2 hours ago</span>
          </div>
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <Tag className="h-4 w-4" />
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-destructive hover:text-destructive"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="flex-1 overflow-auto px-6 py-4">
        <Input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="border-0 px-0 text-2xl font-bold shadow-none focus-visible:ring-0 h-auto"
          placeholder="Note title..."
        />
        <div className="mt-4 flex items-center gap-2">
          <Badge
            variant="outline"
            className="bg-blue-500/10 text-blue-500 border-blue-500/20 cursor-pointer hover:bg-blue-500/20"
          >
            work
          </Badge>
          <Badge
            variant="outline"
            className="bg-green-500/10 text-green-500 border-green-500/20 cursor-pointer hover:bg-green-500/20"
          >
            meeting
          </Badge>
          <Button variant="ghost" size="sm" className="h-6 text-xs gap-1">
            <Tag className="h-3 w-3" />
            Add tag
          </Button>
        </div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          className="mt-6 w-full resize-none border-0 bg-transparent text-base leading-relaxed outline-none placeholder:text-muted-foreground/50 min-h-[300px]"
          placeholder="Start writing..."
        />
      </div>
    </div>
  );
}
