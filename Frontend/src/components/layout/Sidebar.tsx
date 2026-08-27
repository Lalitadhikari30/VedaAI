import { 
  LayoutGrid, Monitor, ClipboardList, FileText, Library, Settings, 
  Sparkles, ChevronsRight, PanelLeftClose, Clock
} from 'lucide-react';
import clsx from 'clsx';

interface SidebarProps {
  collapsed?: boolean;
  onToggle?: () => void;
}

const navItems = [
  { icon: LayoutGrid, label: 'Home', active: false },
  { icon: Monitor, label: 'My Classroom', active: false },
  { icon: ClipboardList, label: 'Assignments', active: false },
  { icon: FileText, label: 'Exams', active: true },
  { icon: Library, label: 'My Library', active: false },
  { icon: Clock, label: 'History', active: false },
];

export default function Sidebar({ collapsed = false, onToggle }: SidebarProps) {
  // Common vertical layout variables:
  // Header: h-16 (64px)
  // Toolkit: py-4 (h-12 button = 48px)
  // Nav: gap-3, each button h-12 (48px)
  // Bottom: py-4, settings button h-12 (48px)
  
  if (collapsed) {
    return (
      <aside className="hidden lg:flex flex-col items-center w-[78px] bg-white border-r border-[#ECECEA] h-screen sticky top-0 z-30 flex-shrink-0 select-none justify-between transition-all duration-200">
        {/* Top Section */}
        <div className="flex flex-col items-center w-full">
          {/* Header Bar: Exact 64px (h-16) */}
          <div className="h-16 w-full flex items-center justify-center border-b border-[#ECECEA] flex-shrink-0">
            <div
              onClick={onToggle}
              title="Expand Sidebar"
              className="w-10 h-10 bg-[#1C1C1C] rounded-2xl flex items-center justify-center shadow-sm flex-shrink-0 cursor-pointer hover:opacity-90 transition-opacity"
            >
              <span className="text-white font-black text-xl leading-none tracking-tighter select-none">V</span>
            </div>
          </div>

          {/* AI Teacher's Toolkit Section: Exact matching height (h-12 button + py-4 container) */}
          <div className="w-full px-3.5 py-4 flex justify-center flex-shrink-0">
            <button
              onClick={onToggle}
              title="AI Teacher's Toolkit"
              className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#D94820] to-[#F27448] text-white flex items-center justify-center shadow-[0_4px_16px_rgba(232,98,60,0.35)] transition-transform hover:scale-105 active:scale-95 cursor-pointer flex-shrink-0"
            >
              <Sparkles size={20} className="text-white" />
            </button>
          </div>

          {/* Icon Navigation Rail: Identical gap-3 and h-12 buttons as expanded */}
          <nav className="flex flex-col items-center gap-3 w-full px-3.5 pt-1">
            {navItems.map((item) => (
              <button
                key={item.label}
                title={item.label}
                className={clsx(
                  'w-12 h-12 rounded-2xl flex items-center justify-center transition-all cursor-pointer group relative flex-shrink-0',
                  item.active
                    ? 'bg-[#FDECE2] text-[#E8623C] border border-[#FAD6C3]/80 shadow-xs'
                    : 'text-[#8A8A87] hover:bg-[#F5F5F3] hover:text-[#1C1C1C]'
                )}
              >
                <item.icon
                  size={21}
                  className={clsx(
                    'transition-colors',
                    item.active ? 'text-[#E8623C]' : 'text-[#8A8A87] group-hover:text-[#1C1C1C]'
                  )}
                />
              </button>
            ))}
          </nav>
        </div>

        {/* Bottom Section: Exact matching layout & height */}
        <div className="flex flex-col items-center gap-3 w-full px-3.5 py-4 border-t border-[#F0F0EE] flex-shrink-0">
          {/* Settings Icon (h-12) */}
          <button
            title="Settings"
            className="w-12 h-12 rounded-2xl flex items-center justify-center text-[#8A8A87] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] transition-colors cursor-pointer flex-shrink-0"
          >
            <Settings size={21} />
          </button>

          {/* School Crest Badge */}
          <div
            title="Universal Public School, New Delhi"
            className="w-11 h-11 rounded-full bg-[#F7F7F5] border border-[#ECECEA] p-1.5 flex items-center justify-center shadow-xs cursor-pointer hover:bg-[#F0F0EE] transition-colors flex-shrink-0"
          >
            <img
              src="/school-crest.svg"
              alt="Universal Public School, New Delhi"
              className="w-full h-full object-contain"
            />
          </div>

          {/* Expand / Toggle Arrow Button */}
          <button
            onClick={onToggle}
            title="Expand Sidebar"
            className="w-9 h-8 rounded-lg flex items-center justify-center text-[#B0B0AD] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] transition-colors cursor-pointer flex-shrink-0 mt-1"
          >
            <ChevronsRight size={18} />
          </button>
        </div>
      </aside>
    );
  }

  // Expanded Sidebar State - Perfectly Levelled with Collapsed State
  return (
    <aside className="hidden lg:flex flex-col w-[276px] bg-white border-r border-[#ECECEA] h-screen sticky top-0 z-30 flex-shrink-0 select-none justify-between transition-all duration-200 shadow-[1px_0_6px_rgba(0,0,0,0.02)]">
      {/* Top Section */}
      <div className="flex flex-col w-full min-h-0 overflow-y-auto">
        {/* Header Bar: Exact 64px (h-16) */}
        <div className="h-16 px-5 border-b border-[#ECECEA] flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-[#1C1C1C] rounded-2xl flex items-center justify-center shadow-sm flex-shrink-0">
              <span className="text-white font-black text-xl leading-none tracking-tighter">V</span>
            </div>
            <span className="text-[#1C1C1C] font-extrabold text-[20px] tracking-tight">VedaAI</span>
          </div>

          <button
            onClick={onToggle}
            title="Collapse Sidebar"
            className="w-8 h-8 rounded-lg flex items-center justify-center text-[#8A8A87] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] transition-colors cursor-pointer flex-shrink-0"
          >
            <PanelLeftClose size={18} />
          </button>
        </div>

        {/* AI Teacher's Toolkit Section: Exact matching height (h-12 button + py-4 container) */}
        <div className="w-full px-3.5 py-4 flex-shrink-0">
          <button className="w-full h-12 px-4 rounded-2xl bg-gradient-to-tr from-[#D94820] to-[#F27448] text-white font-bold text-[14.5px] flex items-center justify-center gap-2.5 shadow-[0_4px_16px_rgba(232,98,60,0.35)] hover:brightness-110 active:scale-[0.98] transition-all cursor-pointer">
            <Sparkles size={19} className="text-white flex-shrink-0" />
            <span>AI Teacher's Toolkit</span>
          </button>
        </div>

        {/* Navigation List: Identical gap-3 and h-12 buttons as collapsed */}
        <nav className="flex flex-col gap-3 w-full px-3.5 pt-1">
          {navItems.map((item) => (
            <button
              key={item.label}
              className={clsx(
                'w-full h-12 flex items-center gap-3.5 px-4 rounded-2xl text-[14.5px] font-semibold transition-all text-left cursor-pointer group flex-shrink-0',
                item.active
                  ? 'bg-[#FDECE2] text-[#E8623C] border border-[#FAD6C3]/80 shadow-xs'
                  : 'text-[#6B6B68] hover:bg-[#F5F5F3] hover:text-[#1C1C1C]'
              )}
            >
              <item.icon
                size={21}
                className={clsx(
                  'transition-colors flex-shrink-0',
                  item.active ? 'text-[#E8623C]' : 'text-[#8A8A87] group-hover:text-[#1C1C1C]'
                )}
              />
              <span className="tracking-tight">{item.label}</span>
            </button>
          ))}
        </nav>
      </div>

      {/* Bottom Section: Exact matching layout & height */}
      <div className="flex flex-col gap-3 w-full px-3.5 py-4 border-t border-[#F0F0EE] flex-shrink-0">
        {/* Settings Item (h-12) */}
        <button className="w-full h-12 flex items-center gap-3.5 px-4 rounded-2xl text-[14.5px] font-semibold text-[#6B6B68] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] transition-all text-left cursor-pointer flex-shrink-0">
          <Settings size={21} className="flex-shrink-0 text-[#8A8A87]" />
          <span className="tracking-tight">Settings</span>
        </button>

        {/* School Card: Universal Public School, New Delhi */}
        <div className="p-3 bg-[#F7F7F5] rounded-2xl border border-[#ECECEA] flex items-center gap-3 shadow-xs">
          <div className="w-10 h-10 rounded-full bg-white flex items-center justify-center border border-[#ECECEA] p-1.5 flex-shrink-0">
            <img
              src="/school-crest.svg"
              alt="Universal Public School"
              className="w-full h-full object-contain"
            />
          </div>
          <div className="min-w-0">
            <div className="text-[13.5px] font-bold text-[#1C1C1C] truncate leading-snug">Universal Public School</div>
            <div className="text-[12px] text-[#8A8A87] font-medium truncate">New Delhi</div>
          </div>
        </div>
      </div>
    </aside>
  );
}
